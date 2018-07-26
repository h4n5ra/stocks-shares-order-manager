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

public class SampleClient implements Client {

    // a list of instruments
    // TODO: add more instruments
    private static final Instrument[] INSTRUMENTS = {new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
    //stores the order ID as a key and the order itself as a value.
    private final HashMap<String, NewOrderSingle> OUT_QUEUE = new HashMap();
    //id stores an id number for the messages so that unique output messages are identifiable
    private int id = 0;
    private Socket orderManagerConnection; //connection to order manager
    private boolean stop;

    //All this constructor does is accept a connection to the order manager and print out the port which it connected to.
    public SampleClient(int port, boolean stop) throws IOException {
        this.stop = stop;
        orderManagerConnection = new ServerSocket(port).accept();
        MyLogger.out("OM connected to client port " + port);
    }

    @Override
    public int sendOrder(int size, int instrid, char side) throws IOException {
        //NewOrderSingle is a class that stores an order with a size, instrument ID and buy/sell.
        //the ID of the instrument is its position in the instrument[] array
        //TODO: implement RIC code instead of instrument ID
        NewOrderSingle nos = new NewOrderSingle(size, instrid, INSTRUMENTS[instrid]);

        //queues the order
        OUT_QUEUE.put("" + id, nos);

        if (orderManagerConnection.isConnected()) {
            //sends FixTag object to the order manager
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
//          OrderManager.makeClientRequest();
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