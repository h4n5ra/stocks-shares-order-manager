import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

import javax.net.ServerSocketFactory;

import OrderManager.OrderManager;
import OrderRouter.Router;
import Ref.Instrument;
import Ref.Ric;
import Tools.MyLogger;
import org.apache.log4j.Level;

/* Implements the interface Router and runs as a thread. Reads the input stream that is coming in and selects what to do
depending on the message it receives from the OrderManager. There is a method in the interface Router that should be
implemented here but it should be sending a cancel order back to the OrderManager after receiving it from the outside
world that has not yet been implemented.
 */

public class SampleRouter extends Thread implements Router{
	private Socket orderManagerConnection;
	private int port;
	private boolean stop;
	private ObjectInputStream is;
	private ObjectOutputStream os;

	// Initialises the sample router with a name and a port.
	public SampleRouter(String name,int port, boolean stop){
		this.setName(name);
		this.port=port;
		this.stop = stop;
	}

	public void run() {
		try {
			// OrderManager connects us the port of this SampleRouter.
			orderManagerConnection=ServerSocketFactory.getDefault().createServerSocket(port).accept();

			int count = 0;
			while(count<30 || !stop) {

				if(0 < orderManagerConnection.getInputStream().available()){
					// If the input stream does contain something we get the object and read it
					// method we found and do different action depending on it, hence the switch statement.
					is=new ObjectInputStream(orderManagerConnection.getInputStream());
					Router.api methodName=(Router.api)is.readObject();
					MyLogger.out("Received message from OM:     "+methodName);
					switch(methodName) {
						case routeOrder:
							routeOrder(is.readLong(),is.readInt(),is.readInt(),(Instrument)is.readObject());
							break;
						case priceAtSize:
							priceAtSize(is.readLong(),is.readInt(),(Instrument)is.readObject(),is.readInt());
							break;
					}
				}else{
					Thread.sleep(50);
					count++;
				}
			}
			// Catch exceptions if we can't connect to the port through OrderManager
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			MyLogger.out(e.getMessage(), Level.FATAL);
		}
		MyLogger.out("Router is free at last");
	}

	// routeOrder creates an output-stream, adding a newFill with the same information it read as well as creating a
	// random price and a random size for the fill.
	@Override
	public void routeOrder(long id,int sliceId,int size,Instrument i) throws IOException, InterruptedException{
	    //TODO: set fill size
		int fillSize = 0;

		Thread.sleep(10);
		os=new ObjectOutputStream(orderManagerConnection.getOutputStream());
		os.writeObject("newFill");
		os.writeLong(id);
		os.writeInt(sliceId);
		os.writeInt(fillSize);
		os.writeDouble(199);
		os.flush();
	}

	// sendCancel does not do anything
	// TODO: Figure out if sendCancel needs to be implemented or not cause no cancel methods have been implemented
	@Override
	public void sendCancel(long id,int sliceId,int size,Instrument i){
	}


	// priceAtSize creates an output-stream with the same information it read but adding to it that this is the best
	// price of this order of this size.
	@Override
	public void priceAtSize(long id, int sliceId,Instrument i, int size) throws IOException{
		os=new ObjectOutputStream(orderManagerConnection.getOutputStream());
		os.writeObject("bestPrice");
		os.writeLong(id);
		os.writeInt(sliceId);
		os.writeDouble(199);
		os.flush();
	}
}
