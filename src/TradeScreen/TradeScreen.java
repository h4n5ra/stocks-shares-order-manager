package TradeScreen;

import java.io.IOException;

import OrderManager.Order;

/*
This is an interface used by the trader to define what different types of orders the trader should handle.
 */

public interface TradeScreen {
	 enum api{newOrder,price,fill,cross,cancel}
	 void newOrder(long id,Order order) throws IOException, InterruptedException;
	 void acceptOrder(long id) throws IOException;
	 void sliceOrder(long id,int sliceSize) throws IOException;
	 void price(long id,Order o) throws InterruptedException, IOException;
}
