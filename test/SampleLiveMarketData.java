import java.util.Random;

import LiveMarketData.LiveMarketData;
import OrderManager.Order;
import Ref.Instrument;

//TODO: make it a thread
//TODO: get real market data
public class SampleLiveMarketData implements LiveMarketData{

	public void setPrice(Order o){
		o.initialMarketPrice=199;
	}
}
