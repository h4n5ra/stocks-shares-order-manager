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

public class SampleRouter extends Thread implements Router{
	private static final Random RANDOM_NUM_GENERATOR=new Random();
	private static final Instrument[] INSTRUMENTS={new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
	private Socket orderManagerConnection;
	private int port;
	private boolean stop;

	// Initialises the sample router with a name and a port.
	public SampleRouter(String name,int port, boolean stop){
		this.setName(name);
		this.port=port;
		this.stop = stop;
	}

	ObjectInputStream is;
	ObjectOutputStream os;

	public void run() {
		//OM will connect to us
		try {
			// OrderManager connects us the port of this SampleRouter.
			orderManagerConnection =ServerSocketFactory.getDefault().createServerSocket(port).accept();

			// Running this while-loop indefinitely. It checks for an inputstream and if it doesn't find one it sleeps
			// 100ms
			int count = 0;
			while(true && (count<30 || !stop)) {

				if(0< orderManagerConnection.getInputStream().available()){
					// If the input stream does contain something we get the method from in and tell the user what
					// method we found and do different action depending on it, hence the switch statement.
					is=new ObjectInputStream(orderManagerConnection.getInputStream());
					Router.api methodName=(Router.api)is.readObject();
					MyLogger.out("Recieved message from OM:     "+methodName);

					// We switch on the method name. Note there is nothing done here if it should cancel the order
					// The cancel method has not been implemented but it is define in the Router interface.
					// TODO: Put a default case to do, ie. cancel in the interface, but never used...
					switch(methodName){
						case routeOrder:
							routeOrder(is.readLong(),is.readInt(),is.readInt(),(Instrument)is.readObject());
							break;
						case priceAtSize:
							priceAtSize(is.readLong(),is.readInt(),(Instrument)is.readObject(),is.readInt());
							break;
					}
				}else{
					Thread.sleep(500);
					count++;
				}
			}
			// Catch exceptions if we can't connect to the port through OrderManager
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MyLogger.out("Router is free at last");
	}

	// routeOrder creates an output-stream, adding a newFill with the same information it read as well as creating a
	// random price and a random size for the fill.
	@Override
	public void routeOrder(long id,int sliceId,int size,Instrument i) throws IOException, InterruptedException{
		int fillSize=RANDOM_NUM_GENERATOR.nextInt(size);
		//TODO have this similar to the market price of the instrument

//        OrderManager.makeRouterRequest();
		double fillPrice=199;//*RANDOM_NUM_GENERATOR.nextDouble();
		Thread.sleep(42);
		os=new ObjectOutputStream(orderManagerConnection.getOutputStream());
		os.writeObject("newFill");
		os.writeLong(id);
		os.writeInt(sliceId);
		os.writeInt(fillSize);
		os.writeDouble(fillPrice);
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
//        OrderManager.makeRouterRequest();
		os=new ObjectOutputStream(orderManagerConnection.getOutputStream());
		os.writeObject("bestPrice");
		os.writeLong(id);
		os.writeInt(sliceId);
		os.writeDouble(199);//*RANDOM_NUM_GENERATOR.nextDouble());
		os.flush();
	}
}
