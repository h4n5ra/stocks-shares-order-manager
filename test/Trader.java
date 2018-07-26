import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import javax.net.ServerSocketFactory;

import OrderManager.Order;
import OrderManager.OrderManager;
import TradeScreen.TradeScreen;
import Tools.MyLogger;
import org.apache.log4j.Level;

/*
Trader is a class that has its own run method as it extends threads. It mocks how a trader should act
by reading it from an input stream data and handle it appropriately according to the message received.
If it accepts the order it adds it to its list of orders, otherwise it is writing to the output stream
different messages to be executed by the OrderManager.
*/

public class Trader extends Thread implements TradeScreen {
	private HashMap<Long,Order> orders=new HashMap<Long,Order>();
	private static Socket omConn;
	private int port;
	private boolean stop;

	Trader(String name,int port, boolean stop){
		this.setName(name);
		this.port=port;
		this.stop = stop;
	}

	ObjectInputStream  is;
	ObjectOutputStream os;
	public void run(){
		//OM will connect to us
		try {
		    // Make a socket connection.
			omConn=ServerSocketFactory.getDefault().createServerSocket(port).accept();
			
			//is=new ObjectInputStream( omConn.getInputStream());
			InputStream s=omConn.getInputStream(); //if i try to create an objectinputstream before we have data it will block
			int count = 0;
            // Keep running and waiting for any data streamed in.

			while(true && (count<50 || !stop)){

				if(0<s.available()){
				    // If data available,
					is=new ObjectInputStream(s);  //TODO check if we need to create each time. this will block if no data, but maybe we can still try to create it once instead of repeatedly
					api method=(api)is.readObject(); // Retrieve the type of data.
					MyLogger.out("      Recieved message from OM:     "+ method);
					switch(method){
						case newOrder:
							newOrder(is.readLong(),(Order)is.readObject());
							break;
						case price:
							price(is.readLong(),(Order)is.readObject());
							break;
						case cross:
							is.readLong();
							is.readObject();
							break; //TODO
						case fill:
							is.readLong();
							is.readObject();
							break; //TODO
						case cancel:
							cancel(is.readLong(), (Order)is.readObject());
							break; //TODO
					}
				}else{
					//MyLogger.out("Trader Waiting for data to be available - sleep 1s");
					count++;
					Thread.sleep(50);
				}
			}
			MyLogger.out("Free from this hellish prison");
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
            MyLogger.out(e.getMessage(), Level.FATAL);
		}
	}

	@Override
	public void newOrder(long id,Order order) throws IOException, InterruptedException {
		//TODO the order should go in a visual grid, but not needed for test purposes
		//Thread.sleep(500);
		orders.put(id, order);
		acceptOrder(id);
	}

	@Override
	public void acceptOrder(long id) throws IOException {
		os=new ObjectOutputStream(omConn.getOutputStream());
		os.writeObject("acceptOrder");
		os.writeLong(id);
		os.flush();
	}

	@Override
	public void sliceOrder(long id, int sliceSize) throws IOException {
		os=new ObjectOutputStream(omConn.getOutputStream());
		os.writeObject("sliceOrder");
		os.writeLong(id);
		os.writeInt(sliceSize);
		os.flush();
	}

	@Override
	public void price(long id,Order o) throws InterruptedException, IOException {
		//TODO should update the trade screen
		sliceOrder(id,orders.get(id).sizeRemaining()/2);
	}

	private void cancel(long id, Order o) {
		orders.remove(id);
	}
}
