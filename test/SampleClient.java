import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import OrderClient.Client;
import OrderClient.NewOrderSingle;
import OrderManager.Order;
import Ref.Instrument;
import Ref.Ric;
import Tools.*;
import org.apache.log4j.Level;

import static java.lang.Thread.sleep;

/* Sample client implements the client interface. It uses the messageHandler to decide what to do with each type of
message it receives through the fix tag.
 */

public class SampleClient extends Mock implements Client {

    //Initializing member variables - first is just a random number. Second is the instruments associated with the client;
    //in this instance these are hard-coded because this is a test, but they (presumably) need to be able to change.
    //The socket is just a way for it to connect for the order manager.
    //Out queue stores the order ID as a key and the order itself as a value.
    //id just stores an id number for the messages so that unique output messages are identifiable
    private static final Instrument[] INSTRUMENTS = {new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
    private final HashMap<String, NewOrderSingle> OUT_QUEUE = new HashMap(); //queue for outgoing orders

    private int id = 0; //message id number
    private Socket orderManagerConnection; //connection to order manager
    private boolean stop;

    //All this constructor does is accept a connection to the order manager and print out the port which it connected to.
    public SampleClient(int port, boolean stop) throws IOException {
        //OM will connect to us
        this.stop = stop;
        orderManagerConnection = new ServerSocket(port).accept();
        MyLogger.out("OM connected to client port " + port);
    }

    //The random.nextInt method just returns a random number between 0 and the argument inclusive;
    //so here the sendOrder method chooses a random instrument from the list and assigns it to the field instrument.
    //NewOrderSingle is a class that stores an order with a size, a price and an instrument. Here the size is set as
    //random between 0 and 5000, the id is set as a random between 0 and 3, and the instrument is set as above.
    //A new order (NewOrderSingle Object) is created with these parameter.
    @Override
    public int sendOrder(int size, int instrid, char side) throws IOException {
        NewOrderSingle nos = new NewOrderSingle(size, instrid, INSTRUMENTS[instrid]);

        OUT_QUEUE.put("" + id, nos);

        if (orderManagerConnection.isConnected()) {
            ObjectOutputStream os = new ObjectOutputStream(orderManagerConnection.getOutputStream());
            IFixTag tag = FixTagFactory.makeNewOrderSingle(id, INSTRUMENTS[instrid].toString(), side, size);
            MyLogger.out("Sent new order single to OM:  " + tag.getCOrderId());
            tag.send(os);
            os.flush();
        }
        //Increments message id so that each message has a unique id.
        return id++;
    }

    @Override
    public void sendCancel(int idToCancel) throws IOException {
        if (OUT_QUEUE.get("" + idToCancel).getStatus() == FixTagRef.PENDING_NEW) {
            MyLogger.out("Cannot cancel pending new order");
            return;
        }
        if (orderManagerConnection.isConnected()) {
            ObjectOutputStream os = new ObjectOutputStream(orderManagerConnection.getOutputStream());
            long OMOrderID = OUT_QUEUE.get("" + idToCancel).getOMOrderID();
            IFixTag tag = FixTagFactory.makeCancelRequest(idToCancel, OMOrderID);
//            OrderManager.makeClientRequest();
            tag.send(os);
            MyLogger.out("Sent cancel request to OM:    " + tag.getCOrderId() + " " + tag.getOMOrderId());
            os.flush();
        }
    }

    @Override
    public void messageHandler() {
        //Method gets an input stream from the connection to the order manager; reads it as a string so that we know
        //a fix is happening, and which thread is responsible. The fixes are tagged into a list, presumably they're seperated
        //by semi colons, which is how the regex splits them.
        (new MessageHandler()).start();
    }

    void recieveExecutionUpdate(IFixTag tag) {
        MyLogger.out("Recieved update from OM:      " + tag.getCOrderId() + " " + tag.getOMOrderId() + " " + tag.getOrdStatus());
        NewOrderSingle nos = OUT_QUEUE.get("" + tag.getCOrderId());
        nos.setOMOrderID(tag.getOMOrderId());
        nos.setStatus(tag.getOrdStatus());
        if (tag.getOrdStatus() == FixTagRef.CANCELLED) {
            OUT_QUEUE.remove(tag.getCOrderId());
            MyLogger.out("Order cancelled: " + tag.getCOrderId() + " " + tag.getOMOrderId());
        }
    }

    public class MessageHandler extends Thread {
        public void run() {
            ObjectInputStream is;
            try {
                int count = 0;
                while (count < 50 || !stop) {
                    //is.wait(); //this throws an exception!!
                    while (0 < orderManagerConnection.getInputStream().available()) {
                        is = new ObjectInputStream(orderManagerConnection.getInputStream());
                        IFixTag fixTag = FixTagFactory.read(is);
                        if (fixTag.getMsgType() == FixTagRef.EXECUTION_REPORT) {
                            recieveExecutionUpdate(fixTag);
                        } else {
                            MyLogger.out("Unknown fixTag recieved");
                        }
                    }
                    sleep(50);
                    count++;
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                MyLogger.out(e.getMessage(), Level.FATAL);
            }
        }
    }
}