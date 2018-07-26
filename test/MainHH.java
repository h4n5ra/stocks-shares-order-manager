import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;

import LiveMarketData.LiveMarketData;
import OrderManager.OrderManager;
import Tools.MyLogger;
import org.apache.log4j.Level;

/*this class provides a runnable test program that runs a manager and some clients and routers using command line
arguments about the orders that the clients make.
 */
public class MainHH{

	// gets date for log files.
	static{
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hhmmss");
		System.setProperty("current_date", dateFormat.format(new Date()));
	}

	public static void main(String[] args) throws InterruptedException {
		MyLogger.init();
		//long time = System.nanoTime();
		MyLogger.out("TEST: this program tests ordermanager");

		//Mockclient creates a command-line 'menu' which asks user for order information
		MockClientHH mc1 = new MockClientHH("Client 1", 2000);

		//makes the socket for the ordermanager to connect to the clients. note the port matches the client port
		InetSocketAddress[] clients = {new InetSocketAddress("localhost", 2000)};

		//makes 2 "router threads" and starts them.
		//the threads are SampleRouter which implement the router interface
		//the threads infinitely listen for messages
		SampleRouter sr1 = (new SampleRouter("Router LSE", 2010, false));
		sr1.start();
		SampleRouter sr2 = (new SampleRouter("Router BATE", 2011, false));
		sr2.start();

		//makes the socket for the ordermanager to connect to the routers. note the ports match the router ports
		InetSocketAddress[] routers = {new InetSocketAddress("localhost", 2010), new InetSocketAddress("localhost", 2011)};

		//makes a trader thread and starts it.
		//the Trader implements the tradeScreen interface
		//the trader thread also infinitely listens for messages
		Trader tr = (new Trader("Trader James", 2020, false));
		tr.start();

		//makes the socket for the ordermanager to connect to the trader. note the matching port
		InetSocketAddress trader = new InetSocketAddress("localhost", 2020);

		//makes a SampleLiveMarketData object that implements the LiveMarketData interface.
		//is used to set the price on orders
		LiveMarketData liveMarketData = new SampleLiveMarketData();

		//creates and starts the order manager thread
		MockOMHH mo = (new MockOMHH("Order Manager", routers, clients, trader, liveMarketData));
		mo.start();

		// starts remaining threads
        mc1.start();
		mc1.join();
		sr1.join();
		sr2.join();
		tr.join();
		mo.join();
		//System.out.println((float) (System.nanoTime()-time)/(1000*1000*1000));
	}
}


//Mockclient is a thread makes a SampleClient on a specified port. Then it makes the client send some orders.
class MockClientHH extends Thread {

    int port;
    String name;
    final static Object inputReadLock = new Object(); // ensures concurrent clients do not read each others input

    MockClientHH(String name, int port) {
        this.port = port;
        this.name = name;
        this.setName(name);
    }

    public void run() {
        int size;
        int instrid;
        int orderid;
        String option;
        SampleClient client = null;
        try {
            client = new SampleClient(port, false);
            client.messageHandler();
        } catch (IOException e) {
            MyLogger.out(e.getMessage(), Level.FATAL);
        }
        while (true) {
            try {
                // ensures more than one client's menu isn't displayed at the same time causing inputs to be mixed up
                synchronized (inputReadLock) {
                    System.out.println("");
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Form for " + name);
                    System.out.print("Buy, Sell, Cancel or Exit? ");
                    option = scanner.next().toLowerCase(); //makes option case-insensitive
                    if (option.equals("buy")) {
                        System.out.print("Enter buy order size: ");
                        try {
                            size = scanner.nextInt();
                            System.out.print("Enter instrument ID: ");
                            instrid = scanner.nextInt();
                            client.sendOrder(size, instrid, '1');
                        }catch (InputMismatchException e){
                            System.out.println("Please enter a valid buy order");
                            System.out.println("Order size and instrument ID must be integers");
                            System.out.println("Restarting form...");
                            scanner.reset();
                        }
                    } else if (option.equals("sell")) {
                        try{
                            System.out.print("Enter sell order size: ");
                            size = scanner.nextInt();
                            System.out.print("Enter instrument ID: ");
                            instrid = scanner.nextInt();
                            client.sendOrder(size, instrid, '2');
                        }catch (InputMismatchException e){
                            System.out.println("Please enter a valid sell order");
                            System.out.println("Order size and instrument ID must be integers");
                            System.out.println("Restarting form...");
                            scanner.reset();
                        }
                    } else if (option.equals("cancel")) {
                        try {
                            System.out.print("Enter order ID for the order you would like to cancel: ");
                            orderid = scanner.nextInt();
                            client.sendCancel(orderid);
                        }catch(InputMismatchException e){
                            System.out.println("Please enter an integer as the order ID");
                            System.out.println("Restarting form...");
                            scanner.reset();
                        }
                    } else if (option.equals("exit")) {
                        System.exit(0);
                    } else {
                        System.out.println("Please enter a valid command");
                        System.out.println("Either buy, sell, cancel or exit");
                        System.out.println("Restarting form...");
                        scanner.reset();
                    }
                    System.out.println("");
                    Thread.sleep(4000);
                }
            } catch (IOException | InterruptedException e) {
                MyLogger.out(e.getMessage(), Level.FATAL);
            }
        }
    }
}

//MockOm is a thread that makes an ordermanager and runs it.
//ordermanager runs in it's constructor which is rather silly!
class MockOMHH extends Thread{
	InetSocketAddress[] clients;
	InetSocketAddress[] routers;
	InetSocketAddress trader;
	LiveMarketData liveMarketData;
	MockOMHH(String name, InetSocketAddress[] routers, InetSocketAddress[] clients, InetSocketAddress trader, LiveMarketData liveMarketData){
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
			OrderManager m = new OrderManager(routers,clients,trader,liveMarketData, false);//the manager runs forever in its constructor.
            m.mainLoop();
		}catch(IOException | ClassNotFoundException | InterruptedException ex){
			MyLogger.out(ex.getMessage(), Level.FATAL);//.log(Level.SEVERE,null,ex);
		}
	}
}