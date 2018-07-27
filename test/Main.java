import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import LiveMarketData.LiveMarketData;
import OrderManager.OrderManager;
import Ref.Instrument;
import Ref.Ric;
import Tools.MyLogger;
import org.apache.log4j.Level;

// This class provides a runnable test program that runs a manager and some clients and routers
public class Main{

	// Get date for log files.
	static{
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HHmmss");
		System.setProperty("current_date", dateFormat.format(new Date()));
	}

	public static void main(String[] args) throws IOException, InterruptedException {
            MyLogger.init();
            long time = System.nanoTime();
            System.out.println(Trader.class);
            MyLogger.out("TEST: this program tests ordermanager");
            //makes 2 "client threads" and starts them.
            //the threads make SampleClient objects and run some methods on those objects
            MockClient mc1 = new MockClient("Client 1", 2000);
            mc1.start();
            MockClient mc2 = (new MockClient("Client 2", 2001));
            mc2.start();

            //makes the socket for the ordermanager to connect to the clients. note the matching ports
            InetSocketAddress[] clients = {new InetSocketAddress("localhost", 2000), new InetSocketAddress("localhost", 2001)};

            //makes 2 "router threads" and starts them.
            //the threads are SampleRouter which implement the router interface
            //the threads infinitely listen for messages
            SampleRouter sr1 = (new SampleRouter("Router LSE", 2010, true));
            sr1.start();
            SampleRouter sr2 = (new SampleRouter("Router BATE", 2011, true));
            sr2.start();

            //makes the socket for the ordermanager to connect to the routers. note the matching ports
            InetSocketAddress[] routers = {new InetSocketAddress("localhost", 2010), new InetSocketAddress("localhost", 2011)};

            //makes a trader thread and starts it.
            //the Trader implements the tradeScreen interface
            //the trader thread also infinitely listens for messages
            Trader tr = (new Trader("Trader James", 2020, true));
            tr.start();

            //makes the socket for the ordermanager to connect to the trader. note the matching port
            InetSocketAddress trader = new InetSocketAddress("localhost", 2020);

            //makes a SampleLiveMarketData object that implements the LiveMarketData interface.
            //is used to set the price on orders
            LiveMarketData liveMarketData = new SampleLiveMarketData();
            //start the order manager thread
            MockOM mo = (new MockOM("Order Manager", routers, clients, trader, liveMarketData));
            mo.start();

            mc1.join();
            mc2.join();
            sr1.join();
            sr2.join();
            tr.join();
            mo.join();
            System.out.println((float) (System.nanoTime() - time) / (1000 * 1000 * 1000));
	}
}


//Mockclient is a thread makes a SampleClient on a specified port. Then it makes the client send some orders.
class MockClient extends Thread{
	int port;
	private static final Random RANDOM_NUM_GENERATOR = new Random();
	private static final Instrument[] INSTRUMENTS = {new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
	MockClient(String name,int port){
		this.port=port;
		this.setName(name);
	}
	public void run() {
		int size = RANDOM_NUM_GENERATOR.nextInt(5000);
		int instid = RANDOM_NUM_GENERATOR.nextInt(3);
		try {
			SampleClient client=new SampleClient(port, true);
            client.messageHandler();
			if(port==2000){//makes one of the clients in the main method do something different to the other one
				//TODO why does this take an arg?
				client.sendOrder(size, instid, '2');
				client.sendOrder(size, instid, '1');
				client.sendOrder(size, instid, '2');
				int id=client.sendOrder(size, instid,'2');
				Thread.sleep(300);
				client.sendCancel(0);
				//TODO run tests on sendCancel in MockClient
				 client.sendCancel(id);
			}else{
				client.sendOrder(size, instid, '2');
				client.sendOrder(size, instid, '1');
				client.sendOrder(size, instid, '2');
			}
		} catch (IOException e) {
            MyLogger.out(e.getMessage(), Level.FATAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

//MockOm is a thread that makes an ordermanager and runs it.
class MockOM extends Thread{
	InetSocketAddress[] clients;
	InetSocketAddress[] routers;
	InetSocketAddress trader;
	LiveMarketData liveMarketData;
	MockOM(String name,InetSocketAddress[] routers,InetSocketAddress[] clients,InetSocketAddress trader,LiveMarketData liveMarketData){
		this.clients=clients;
		this.routers=routers;
		this.trader=trader;
		this.liveMarketData=liveMarketData;
		this.setName(name);
	}
	@Override
	public void run(){
		try{
			//In order to debug constructors you can do F5 F7 F5
			new OrderManager(routers,clients,trader,liveMarketData, true).mainLoop();//the manager runs forever in its constructor.
		}catch(InterruptedException ex){
			MyLogger.out(ex.getMessage(), Level.FATAL);//.log(Level.SEVERE,null,ex);
		}
	}
}