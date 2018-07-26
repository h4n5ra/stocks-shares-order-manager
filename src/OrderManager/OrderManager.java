package OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import Database.Database;
import LiveMarketData.LiveMarketData;
import OrderRouter.Router;
import Ref.Instrument;
import Ref.Ric;
import Tools.FixTagFactory;
import Tools.FixTagRef;
import Tools.IFixTag;
import TradeScreen.TradeScreen;
import Tools.MyLogger;
import org.apache.log4j.Level;

import static java.lang.Thread.sleep;

public class OrderManager {

    /*
        FixTag message type Reference: TODO replace this with a proper reference file?
     */
    private static final char newOrderSingle = 'D';
    private static final char cancelOrderRequest = 'F';
    private static final char executionReport = '8';

    private static LiveMarketData liveMarketData;
    private HashMap<Long, Order> orders = new HashMap<Long, Order>(); //debugger will do this line as it gives state to the object
    //currently recording the number of new order messages we get. TODO why? use it for more?
    private long id = 0; //debugger will do this line as it gives state to the object
    private Socket[] orderRouters; //debugger will skip these lines as they dissapear at compile time into 'the object'/stack
    private Socket[] clients;
    private Socket trader;
    private final Object lock = new Object();
    private boolean stop;

    private static boolean clientRequest = false;
    private static boolean traderRequest = false;
    private static boolean routerRequest = false;

    // Loop variables
    int clientId, routerId;
    Socket router;
    ObjectInputStream inputStream;

    // Attempts to create and return a socket to the given InetSocketAddress.
    // Makes 600 attempts before it fails.
    private Socket connect(InetSocketAddress location) throws InterruptedException {
        boolean connected = false;
        int tryCounter = 0;
        while (!connected && tryCounter < 600) {
            try {
                Socket s = new Socket(location.getHostName(), location.getPort());
                s.setKeepAlive(true);
                return s;
            } catch (IOException e) {
                sleep(500);
                tryCounter++;
            }
        }
        MyLogger.out("Failed to connect to " + location.toString());
        return null;
    }

    private void initialiseSockets(Socket[] sockets, InetSocketAddress[] addresses)
            throws InterruptedException {
        for (int i = 0; i < sockets.length; i++) {
            sockets[i] = connect(addresses[i]);
        }
    }

    /*
     * A constructor for the class, which does the following:
     * 1) Sets member variables.
     * 2) Set up a connection to the trader.
     * 3) Creates and sets up Order Router sockets.
     * 4) Creates and sets up Client connection sockets.
     * 5) Processes messages (scroll down for more detail).
     * */
    public OrderManager(InetSocketAddress[] orderRouters, InetSocketAddress[] clients,
                        InetSocketAddress trader, LiveMarketData liveMarketData, boolean stop)
            throws IOException, ClassNotFoundException, InterruptedException {
        this.liveMarketData = liveMarketData;
        this.trader = connect(trader);

        this.orderRouters = new Socket[orderRouters.length];
        initialiseSockets(this.orderRouters, orderRouters);

        this.clients = new Socket[clients.length];

        initialiseSockets(this.clients, clients);

        this.stop= stop;
    }

    public void mainLoop() throws IOException, ClassNotFoundException, InterruptedException {
        // Main loop of the manager.
        long count = 0;
        while ( count< 50 || !stop) {
            long start = System.nanoTime();
            clientLoop();
            long duration = (System.nanoTime() - start) / 1;
            MyLogger.out("Client loop duration: " + duration + "ns", Level.FATAL);
            routerLoop();
            traderLoop();
            count++;
            sleep(500);

        }
        System.out.println("MainLoop Order Manager escaped");
    }

    private void clientLoop() throws IOException, ClassNotFoundException {
        // Check if there is any data in any of the sockets.
//        if(!clientRequest)
//            return;

        for (int clientId = 0; clientId < this.clients.length; clientId++) {
            Socket client = this.clients[clientId];
            // Check if there is any data to be read.
            if (client.getInputStream().available() > 0) {

                inputStream = new ObjectInputStream(client.getInputStream());
                IFixTag fixTag = FixTagFactory.read(inputStream);
                switch(fixTag.getMsgType()) {
                    case newOrderSingle:
                        MyLogger.out("Recieved NOS from client:     " + fixTag.getCOrderId() + " " + id);
                        newOrder(clientId,fixTag.getCOrderId(),fixTag.getRic(),fixTag.getSide(),fixTag.getQuantity());
                        break;
                    case cancelOrderRequest:
                        MyLogger.out("Recieved cancel from client:  " + fixTag.getCOrderId() + " " + fixTag.getOMOrderId());
                        cancelOrder(clientId,fixTag.getCOrderId(),fixTag.getOMOrderId());
                        break;
                    default:
                        MyLogger.out("Unknown message type: " + fixTag.getMsgType());

                }
            }
        }

//        clientRequest = false;
    }

    private void routerLoop() throws IOException, ClassNotFoundException {
//        if(!routerRequest)
//            return;
        for (routerId = 0; routerId < this.orderRouters.length; routerId++) {
            router = this.orderRouters[routerId];
            // If data is available to be read in.
            if (router.getInputStream().available() > 0) {
                ObjectInputStream inputStream = new ObjectInputStream(router.getInputStream());
                String method = (String) inputStream.readObject();
                MyLogger.out("Recieved message from Router: " + method);

                switch (method) {
                    case "bestPrice":
                        long orderId = inputStream.readLong();
                        int sliceId = inputStream.readInt();
                        Order slice = orders.get(orderId).slices.get(sliceId);
                        slice.bestPrices[routerId] = inputStream.readDouble();
                        slice.bestPriceCount++;
                        if (slice.bestPriceCount == slice.bestPrices.length)
                            reallyRouteOrder(sliceId, slice);
                        break;
                    case "newFill":
                        newFill(inputStream.readLong(), inputStream.readInt(), inputStream.readInt(),
                                inputStream.readDouble());
                        break;
                    default:
                        MyLogger.out("Unexpected method: " + method);
                        break;
                }
            }
        }

//        routerRequest = false;
    }

    private void traderLoop() throws IOException, ClassNotFoundException {
//        if(!traderRequest)
//            return;
        if (this.trader.getInputStream().available() > 0) {
            ObjectInputStream inputStream = new ObjectInputStream(this.trader.getInputStream());
            String method = (String) inputStream.readObject();
            MyLogger.out("Recieved message from trader: " + method);
            switch (method) {
                case "acceptOrder":
                    acceptOrder(inputStream.readLong());
                    break;
                case "sliceOrder":
                    sliceOrder(inputStream.readLong(), inputStream.readInt());
                    break;
                default:
                    MyLogger.out("Unexpected method: " + method);
                    break;
            }
        }

//        traderRequest = false;
    }

//    synchronized public static void makeClientRequest() { clientRequest = true; }
//    synchronized public static void makeTraderRequest() { traderRequest = true; }
//    synchronized public static void makeRouterRequest() { routerRequest = true; }



    // Create a new order
    //TODO use side to determine if the new order is a buy or a sell (at the moment assumes sell)
    private void newOrder(long clientId, long clientOrderId, Ric ric, char side, int quantity) throws IOException {
        orders.put(id, new Order(clientId, clientOrderId, new Instrument(ric), quantity, side));
        if(side == '1') {
            MyLogger.out("New order with side : BUY");
        }  else if (side == '2') {
            MyLogger.out("New order with side: SELL");
        } else {
            MyLogger.out("Something went wrong god help us all");
        }
        //TODO the order manager should send a pending new execution report to the client at this point
        sendUpdateToClient(orders.get(id));

        //send the new order to the trading screen
        sendOrderToTrader(id, orders.get(id), TradeScreen.api.newOrder);
        //send the new order to the trading screen
        //don't do anything else with the order, as we are simulating high touch orders and so need to wait for the trader to accept the order
        id++;
    }

    private void sendUpdateToClient(Order o) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(clients[(int) o.clientid].getOutputStream());
        IFixTag tag = FixTagFactory.makeExecutionReport(o.id,o.clientOrderID, o.OrdStatus);
        tag.send(os);
        os.flush();
        MyLogger.out("Sent update to client:        " + tag.getCOrderId() + " " + tag.getOMOrderId());
    }

    private void sendOrderToTrader(long id, Order o, Object method) throws IOException {
        ObjectOutputStream ost = new ObjectOutputStream(trader.getOutputStream());
        ost.writeObject(method);
        ost.writeLong(id);
        ost.writeObject(o);
        ost.flush();
    }

    public void acceptOrder(long id) throws IOException {
        Order o = orders.get(id);
        if (o.OrdStatus != FixTagRef.PENDING_NEW) { //Pending New
            MyLogger.out("error accepting order that has already been accepted");
            return;
        }
        o.OrdStatus = FixTagRef.NEW; //New
        sendUpdateToClient(o);
        price(id, o);
    }

    // Slice order into multiple smaller orders.
    public void sliceOrder(long id, int sliceSize) throws IOException {
        Order o = orders.get(id);
        //slice the order. We have to check this is a valid size.
        //Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated with either a child order or the original order
        if (o.sizeRemaining() - o.sliceSizes() < sliceSize) {
            MyLogger.out("error sliceSize is bigger than remaining size to be filled on the order");
            return;
        }
        int sliceId = o.newSlice(sliceSize);
        Order slice = o.slices.get(sliceId);
        internalCross(id, slice);
        int sizeRemaining = o.slices.get(sliceId).sizeRemaining();
        if (sizeRemaining > 0) {
            routeOrder(id, sliceId, sizeRemaining, slice);
        }
    }

    // Attempt to match orders.
    private void internalCross(long id, Order o) throws IOException {
        for (Map.Entry<Long, Order> entry : orders.entrySet()) {
            if (entry.getKey().longValue() == id) continue;
            Order matchingOrder = entry.getValue();

            if (!(matchingOrder.instrument.toString().equals(o.instrument.toString()) && matchingOrder.initialMarketPrice == o.initialMarketPrice &&
                    matchingOrder.side != o.side))
                continue;
            System.out.println("TESTING CROSS: " + o.side + " : " + matchingOrder.side);
            System.out.println("TESTING CROSS: " + o.instrument + " : " + matchingOrder.instrument);
            System.out.println("TESTING CROSS: " + o.initialMarketPrice + " : " + matchingOrder.initialMarketPrice);
            System.out.println("REMAINING 1: " + o.sizeRemaining());
            System.out.println("REMAINING 2: " + matchingOrder.sizeRemaining());
            //TODO add support here and in Order for limit orders
            int sizeBefore = o.sizeRemaining();
            o.cross(matchingOrder);
            if (sizeBefore != o.sizeRemaining()) {
                //if order was filled a bit
                sendOrderToTrader(id, o, TradeScreen.api.cross);
            }
        }
    }

    private void setToPendingCancelled(int clientId, long CorderID, long orderID) throws IOException {
        //todo set to pending cancelled
        //in the main loop if you recieve an update on the cancelled order then deal with that
        Order o = orders.get(orderID);
        o.OrdStatus = FixTagRef.PENDING_CANCELLED;
        sendUpdateToClient(orders.get(orderID));
    }

    //clientID is the id of the client
    //CorderID is the "clients order id" i.e. the orderid stored by the client
    //orderID is the id of the original order stored in this OM
    private void cancelOrder(int clientID, long CorderID, long orderID) {
        //TODO implement this
        //get the order from orders based on orderid
        //"CANCEL" it?!?
        //	either cancel in house and send to client or send cancel to router
        //tell the client that it has been cancelled by calling sendCancel
    }

    // Fill order
    private void newFill(long id, int sliceId, int size, double price) throws IOException {
        Order o = orders.get(id);
        o.slices.get(sliceId).createFill(size, price);
        if (o.sizeRemaining() == 0)
            Database.write(o);
        sendOrderToTrader(id, o, TradeScreen.api.fill);
    }

    // Send order to the main router.
    private void routeOrder(long id, int sliceId, int size, Order order) throws IOException {
        for (Socket r : orderRouters) {
            ObjectOutputStream os = new ObjectOutputStream(r.getOutputStream());
            os.writeObject(Router.api.priceAtSize);
            os.writeLong(id);
            os.writeInt(sliceId);
            os.writeObject(order.instrument);
            os.writeInt(size);
            os.flush();
        }
        //need to wait for these prices to come back before routing
        order.bestPrices = new double[orderRouters.length];
        order.bestPriceCount = 0;
    }

    // Finds the best price and then sends to main router.
    private void reallyRouteOrder(int sliceId, Order o) throws IOException {
        //TODO this assumes we are buying rather than selling
        // Is this TODO or comment?
        int minIndex = 0;
        double min = o.bestPrices[0];
        for (int i = 1; i < o.bestPrices.length; i++) {
            if (o.bestPrices[i] < min) {
                minIndex = i;
                min = o.bestPrices[i];
            }
        }
        ObjectOutputStream os = new ObjectOutputStream(orderRouters[minIndex].getOutputStream());
        os.writeObject(Router.api.routeOrder);
        os.writeLong(o.id);
        os.writeInt(sliceId);
        os.writeInt(o.sizeRemaining());
        os.writeObject(o.instrument);
        os.flush();
    }

    private void sendCancel(Order order, Router orderRouter) {
        //orderRouter.sendCancel(order);
        //order.orderRouter.writeObject(order);
    }

    private void price(long id, Order o) throws IOException {
        liveMarketData.setPrice(o);
        sendOrderToTrader(id, o, TradeScreen.api.price);
    }
}