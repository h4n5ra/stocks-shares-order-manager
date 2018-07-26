package OrderClient;

import java.io.IOException;

import OrderManager.Order;

public interface Client{
	//Outgoing messages
	int sendOrder(int size, int instid, char side)throws IOException;//needs to communicate in FIX
	void sendCancel(int id)throws IOException;//needs to communicate in FIX
	
	//Incoming messages
	void partialFill(Order order);//needs to communicate in FIX
	void fullyFilled(Order order);//needs to communicate in FIX
	void cancelled(Order order);//needs to communicate in FIX
	
	void messageHandler();
}