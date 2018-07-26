package OrderManager;

import java.io.Serializable;
import java.util.ArrayList;

import Ref.Instrument;

/*
 This class contains the description of orders and the manipulation that an order can go through.
 Each order can have its own sublist of slices that is created using the methods in this class.
 Each order can also have its own sublist of fills which are the partial fills of that orders slices.
 The methods of this class are as follows:
 sliceSize
 newSlice
 sizeFilled
 sizeRemaining
 price
 createFill
 cross
*/

public class Order implements Serializable{
	public long id;
	public Instrument instrument;
	public double initialMarketPrice = 199;
	public long clientOrderID;
	public char side;
	public int size;
    public ArrayList<Order>slices;
    public ArrayList<Fill>fills;
	public double[] bestPrices;
	public int bestPriceCount;
	public long clientid;
	char OrdStatus='A';
	boolean isRouted = false;
	 //OrdStatus is Fix 39, 'A' is 'Pending New'
	//Status state;

	/*
		this method doesn't return the size of each slice it returns the total size of all the slices combined.
		e.g. an order of 5 sub orders each sized 10 would return 50
	*/
	public int sliceSizes() {
		int totalSizeOfSlices=0;
		for(Order c:slices)totalSizeOfSlices+=c.size;
		return totalSizeOfSlices;
	}

	/*
		This method adds a new slice (which is itself an order) to the order
		returns the number of slices before the new slice was added !?
	 */
	public int newSlice(int sliceSize){
		slices.add(new Order(id, clientid, clientOrderID,instrument,sliceSize, side));
		return slices.size()-1;
	}

	/*
		returns the quantity filled of this order.
		this is comprised of the sum of the fills so far (i.e. completed orders in this order)
		and the size filled of each sub slice (recursive)
	 */
	public int sizeFilled(){
		int filledSoFar=0;
		for(Fill f:fills){
			filledSoFar+=f.size;
		}
		for(Order c:slices){
			filledSoFar+=c.sizeFilled();
		}
		return filledSoFar;
	}

	/*
		returns the size of the order minus the amount of the order filled so far.
	 */
	public int sizeRemaining(){
		return size-sizeFilled();
	}

	/*
		returns the cumulative average price of the order so far.
		if it gets filled in 10 fill, each fill at a different price but averaging 50
		then this returns 50
		it needs to take into account the fills of its slices
	 */
	public float price(){
		//TODO this is buggy as it doesn't take account of slices. Let them fix it
		float sum=0;
		for(Fill fill:fills){
			sum+=fill.price;
		}
		return sum/fills.size();
	}

	/*
		adds a new fill to the order
		updates the status (2 == order filled) (1 == partially filled)
	 */
	public void createFill(int size,double price){
		fills.add(new Fill(size,price));
		if(sizeRemaining()==0){
			OrdStatus='2';
		}else{
			OrdStatus='1';
		}
	}

	/*
		Compares to another order and attempts to match with it if the order matches (i.e. one wants to sell x,
		one wants to buy x, and prices match) then then fill each other. The matchingOrder argument it takes in has
		already been scanned in the class OrderManager that they do match before being sent into the function.
	 */

	public void cross(Order matchingOrder){
		//pair slices first and then parent

		int matchSize;
		int size;
		int mParent;


		for(Order slice:slices){
			if(slice.sizeRemaining()==0)continue;
			//TODO could optimise this to not start at the beginning every time
			for(Order matchingSlice:matchingOrder.slices){
				matchSize=matchingSlice.sizeRemaining();
				if(matchSize==0)continue;
				size=slice.sizeRemaining();
				if(size<=matchSize){
					 slice.createFill(size,initialMarketPrice);
					 matchingSlice.createFill(size, initialMarketPrice);
					 break;
				}
				//size>matchSize
				slice.createFill(matchSize,initialMarketPrice);
				matchingSlice.createFill(matchSize, initialMarketPrice);
			}
			size=slice.sizeRemaining();
			mParent=matchingOrder.sizeRemaining()-matchingOrder.sliceSizes();
			if(size>0 && mParent>0){
				if(size>=mParent){
					slice.createFill(size,initialMarketPrice);
					matchingOrder.createFill(size, initialMarketPrice);
				}else{
					slice.createFill(mParent,initialMarketPrice);
					matchingOrder.createFill(mParent, initialMarketPrice);					
				}
			}
			//no point continuing if we didn't fill this slice, as we must already have fully filled the matchingOrder
			if(slice.sizeRemaining()>0)break;
		}
		if(sizeRemaining()>0){
			for(Order matchingSlice:matchingOrder.slices){
				matchSize = matchingSlice.sizeRemaining();
				if(matchSize ==0)continue;
				size=sizeRemaining();
				if(size<= matchSize){
					 createFill(size,initialMarketPrice);
					 matchingSlice.createFill(size, initialMarketPrice);
					 break;
				}
				//size>matchSize
				createFill(matchSize,initialMarketPrice);
				matchingSlice.createFill(matchSize, initialMarketPrice);
			}
			size=sizeRemaining();
			mParent=matchingOrder.sizeRemaining()-matchingOrder.sliceSizes();
			if(size>0 && mParent>0){
				if(size>=mParent){
					createFill(size,initialMarketPrice);
					matchingOrder.createFill(size, initialMarketPrice);
				}else{
					createFill(mParent,initialMarketPrice);
					matchingOrder.createFill(mParent, initialMarketPrice);					
				}
			}
		}
	}

	//TODO implement cancelling orders in Order
	public void cancel(){
		//state=cancelled
	}

	public void setRouted() {
		isRouted=true;
	}

	public void setNotRouted() {
		isRouted=false;
	}

	public Order(long orderId, long clientId, long clientOrderID, Instrument instrument, int size, char side){
		this.clientOrderID =clientOrderID;
		this.size=size;
		this.clientid=clientId;
		this.instrument=instrument;
		fills=new ArrayList<Fill>();
		slices=new ArrayList<Order>();
		this.side = side;
		this.id = orderId;
	}
}

