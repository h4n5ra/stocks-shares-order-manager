package OrderRouter;

import java.io.IOException;

import OrderManager.Order;
import Ref.Instrument;

/*
Interface for how the router works, used by the SampleRouter. It should have methods a router should use when receiving
information from the outside world, but that has not yet been implemented in this program.
 */

public interface Router {
	 enum api{routeOrder,sendCancel,priceAtSize};
	 void routeOrder(long id,int sliceId,int size,Instrument i) throws IOException, InterruptedException;
	 void sendCancel(long id,int sliceId,int size,Instrument i);
	 void priceAtSize(long id, int sliceId,Instrument i, int size) throws IOException;
	
}
