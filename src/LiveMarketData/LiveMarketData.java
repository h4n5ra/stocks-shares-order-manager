package LiveMarketData;

import OrderManager.Order;
import Ref.Instrument;

/* Interface to represent what should be used as live market data.
Here would be good to implement the data as read from a database of the current live data through a thread and then
have methods in this interface needed for that.
 */


public interface LiveMarketData {
	void setPrice(Order o);
}
