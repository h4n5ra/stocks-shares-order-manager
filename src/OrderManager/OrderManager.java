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
import Tools.*;
import TradeScreen.TradeScreen;
import org.apache.log4j.Level;

import static java.lang.Thread.sleep;

/* OrderManager handles order request by first trying to pair internal orders from its clients before sending it out to
the market itself. It reads and writes to output/input streams to communicate to the trader, the routers and the clients
through socket connections.
 */
public class OrderManager {

    /*
        FixTag message type Reference: TODO replace this with a proper reference file?
     */

    private static LiveMarketData liveMarketData;
    private HashMap<Integer, Order> orders = new HashMap<Integer, Order>();
    //currently recording the number of new order messages we get. TODO why? use it for more?
    private int id = 0;
    private Socket[] orderRouters;
    private Socket[] clients;
    private Socket trader;
    private boolean stop;

    // Loop variables
    int routerId;
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
                MyLogger.out("Failed to establish socket.", Level.FATAL);
                sleep(50);
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
                        InetSocketAddress trader, LiveMarketData liveMarketData, boolean stop) throws InterruptedException {
        this.liveMarketData = liveMarketData;
        this.trader = connect(trader);
        this.orderRouters = new Socket[orderRouters.length];
        this.clients = new Socket[clients.length];
        this.stop = stop;

        initialiseSockets(this.orderRouters, orderRouters);
        initialiseSockets(this.clients, clients);
    }

    public void mainLoop()  {
        // Main loop of the manager.
        try {
            int count = 0;
            while (count < 50 || !stop) {
                clientLoop();
                routerLoop();
                traderLoop();
                count++;
                sleep(50);
            }
        }
        catch (IOException | ClassNotFoundException | InterruptedException e){
            MyLogger.out(e.getMessage(), Level.FATAL);
        }
    }

    // Checks if there is any data in any of the sockets.
    private void clientLoop() throws IOException, ClassNotFoundException {

        for (int clientId = 0; clientId < this.clients.length; clientId++) {
            Socket client = this.clients[clientId];
            if (client.getInputStream().available() > 0) {

                inputStream = new ObjectInputStream(client.getInputStream());
                IFixTag fixTag = FixTagFactory.read(inputStream); //gets the fixtag for the client order
                switch(fixTag.getMsgType()) {
                    case FixTagRef.NEW_ORDER_SINGLE:
                        MyLogger.out("Received New Single Order from client:     " + fixTag.getCOrderId() + " " + id);
                        newOrder(clientId,fixTag.getCOrderId(),fixTag.getRic(),fixTag.getSide(),fixTag.getQuantity());
                        break;
                    case FixTagRef.CANCEL_REQUEST:
                        MyLogger.out("Recieved cancel from client:  " + fixTag.getCOrderId() + " " + fixTag.getOMOrderId());
                        setToPendingCancelled(clientId,fixTag.getCOrderId(),fixTag.getOMOrderId());
                        break;
                    default:
                        MyLogger.out("Unknown message type: " + fixTag.getMsgType());

                }
            }
        }
    }

    private void routerLoop() throws IOException, ClassNotFoundException {
        for (routerId = 0; routerId < this.orderRouters.length; routerId++) {
            router = this.orderRouters[routerId];
            // If data is available to be read in.
            if (router.getInputStream().available() > 0) {
                ObjectInputStream inputStream = new ObjectInputStream(router.getInputStream());
                String method = (String) inputStream.readObject();
                MyLogger.out("Recieved message from Router: " + method);

                switch (method) {
                    case "bestPrice":
                        int orderId = inputStream.readInt();
                        int sliceId = inputStream.readInt();
                        Order o = orders.get(orderId);
                        if(o==null) {
                            MyLogger.out("order no longer exists: " + orderId);
                            break;
                        }
                        Order slice = orders.get(orderId).slices.get(sliceId);
                        slice.bestPrices[routerId] = inputStream.readDouble();
                        slice.bestPriceCount++;
                        if (slice.bestPriceCount == slice.bestPrices.length)
                            reallyRouteOrder(sliceId, slice);
                        break;
                    case "newFill":
                        orderId = inputStream.readInt();
                        o = orders.get(orderId);
                        newFill(orderId, inputStream.readInt(), inputStream.readInt(),
                                inputStream.readDouble());
                        o.setNotRouted();
                        if(o.OrdStatus==FixTagRef.PENDING_CANCELLED) {
                            cancelOrder(o);
                        }
                        break;
                    default:
                        MyLogger.out("Unexpected method: " + method);
                        break;
                }
            }
        }
    }

    private void traderLoop() throws IOException, ClassNotFoundException {
        if (this.trader.getInputStream().available() > 0) {
            ObjectInputStream inputStream = new ObjectInputStream(this.trader.getInputStream());
            String method = (String) inputStream.readObject();
            MyLogger.out("Recieved message from trader: " + method);
            switch (method) {
                case "acceptOrder":
                    int orderId = inputStream.readInt();
                    acceptOrder(orderId);
                    break;
                case "sliceOrder":
                    // read the inputstream with id of order to slice and the size of it
                    int idToSlice = inputStream.readInt();
                    int sizeOfSlice = inputStream.readInt();
                    Order o = orders.get(idToSlice);
                    if(o==null) {
                        MyLogger.out("Order no longer exists: " + idToSlice);
                        break;
                    }
                    int slicedId = sliceOrder(idToSlice, sizeOfSlice);
                    // sliceOrder returns a negative value (and an error message) if we can't do a slice of that size
                    if (slicedId >= 0) {
                        Order slice = orders.get(idToSlice).slices.get(slicedId);
                        // Before we send it out to the trading center we check if we can do an internal cross and send
                        // the rest (if any) out to the trading center.
                        internalCross(idToSlice, slice);
                        int sizeRemaining = slice.sizeRemaining();
                        if (sizeRemaining > 0) {
                            routeOrder(idToSlice, slicedId, sizeRemaining, slice);
                        }
                    }
                    break;
                default:
                    MyLogger.out("Unexpected method: " + method);
                    break;
            }
        }
    }

    // Create a new order
    private void newOrder(int clientId, int clientOrderId, Ric ric, char side, int quantity) throws IOException {
        orders.put(id, new Order(id, clientId, clientOrderId, new Instrument(ric), quantity, side));
        if(side == '1') {
            MyLogger.out("New order with side : BUY");
        }  else if (side == '2') {
            MyLogger.out("New order with side: SELL");
        }
        sendUpdateToClient(orders.get(id));

        //send the new order to the trading screen
        sendOrderToTrader(id, orders.get(id), TradeScreen.api.newOrder);
        id++;
    }

    private void sendUpdateToClient(Order o) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(clients[(int) o.clientid].getOutputStream());
        IFixTag tag = FixTagFactory.makeExecutionReport(o.id,o.clientOrderID, o.OrdStatus);
        tag.send(os);
        os.flush();
        MyLogger.out("Sent update to client:        " + tag.getCOrderId() + " " + tag.getOMOrderId() + " " + tag.getOrdStatus());
    }

    private void sendOrderToTrader(int id, Order o, Object method) throws IOException {
        ObjectOutputStream ost = new ObjectOutputStream(trader.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }
    public void acceptOrder(int id) throws IOException {
        Order o = orders.get(id);
        if(o==null) {
            MyLogger.out("order no longer exists: " + id);
            return;
        }
        if (o.OrdStatus != FixTagRef.PENDING_NEW) { //Pending New
            MyLogger.out("error accepting order that has already been accepted");
            return;
        }
        o.OrdStatus = FixTagRef.NEW; //New
        sendUpdateToClient(o);
        price(id, o);
    }

    // Slice order into multiple smaller orders.
    public int sliceOrder(int id, int sliceSize) throws IOException {
        Order o = orders.get(id);
        //slice the order. We have to check this is a valid size.
        //Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated with either a child order or the original order
        if (o.sizeRemaining() - o.sliceSizes() < sliceSize) {
            MyLogger.out("error sliceSize is bigger than remaining size to be filled on the order");
            return -1;
        }
        int sliceId = o.newSlice(sliceSize);
        return sliceId;
    }

    // Attempt to match orders.
    private void internalCross(int id, Order o) throws IOException {
        for (Map.Entry<Integer, Order> entry : orders.entrySet()) {
            if (entry.getKey().intValue() == id) continue;
            Order matchingOrder = entry.getValue();

            if (!(matchingOrder.instrument.toString().equals(o.instrument.toString()) && matchingOrder.initialMarketPrice == o.initialMarketPrice &&
                    matchingOrder.side != o.side))
                continue;
            int sizeBefore = o.sizeRemaining();
            o.cross(matchingOrder);
            if (sizeBefore != o.sizeRemaining()) {
                //if order was filled a bit
                sendOrderToTrader(id, o, TradeScreen.api.cross);
            }
        }
    }

    private void setToPendingCancelled(int clientId, int CorderID, int orderID) throws IOException {
        //todo set to pending cancelled
        //in the main loop if you recieve an update on the cancelled order then deal with that
        Order o = orders.get(orderID);
        if(o==null) {
            MyLogger.out("order no longer exists: " + orderID);
            return;
        }

        o.OrdStatus = FixTagRef.PENDING_CANCELLED;
        sendUpdateToClient(o);//TODO wait till client is reading updates
        if(!o.isRouted) cancelOrder(o);
    }

    //clientID is the id of the client
    //CorderID is the "clients order id" i.e. the orderid stored by the client
    //orderID is the id of the original order stored in this OM
    private void cancelOrder(Order o) throws IOException {
        o.OrdStatus=FixTagRef.CANCELLED;
        sendUpdateToClient(o);
        sendOrderToTrader(o.id,o,TradeScreen.api.cancel);
        orders.remove(o.id);
        MyLogger.out("Order cancelled: " + o.clientOrderID + " " + o.id);
    }

    // Fill order
    private void newFill(int id, int sliceId, int size, double price) throws IOException {
        Order o = orders.get(id);
        o.slices.get(sliceId).createFill(size, price);
        if (o.sizeRemaining() == 0)
            Database.write(o);
        sendOrderToTrader(id, o, TradeScreen.api.fill);
    }

    // Send order to the main router.
    private void routeOrder(int id, int sliceId, int size, Order order) throws IOException {
        for (Socket r : orderRouters) {
            ObjectOutputStream os = new ObjectOutputStream(r.getOutputStream());
            os.writeObject(Router.api.priceAtSize);
            os.writeInt(id);
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
        o.setRouted();
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
        os.writeInt(o.id);
        os.writeInt(sliceId);
        os.writeInt(o.sizeRemaining());
        os.writeObject(o.instrument);
        os.flush();
    }

    private void sendCancel(Order order, Router orderRouter) {
        //orderRouter.sendCancel(order);
        //order.orderRouter.writeObject(order);
    }

    private void price(int id, Order o) throws IOException {
        liveMarketData.setPrice(o);
        sendOrderToTrader(id, o, TradeScreen.api.price);
    }
}