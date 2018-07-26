package OrderClient;

import java.io.IOException;

import OrderManager.Order;

/* Client is an interface to use for making theoretical clients like SampleClient. Partially filled, fully filled and
cancelled orders are handled by the messageHandler.
 */

public interface Client{
	//Outgoing messages
	int sendOrder(int size, int instid, char side)throws IOException;
	void sendCancel(int id)throws IOException;
	
	void messageHandler();
}