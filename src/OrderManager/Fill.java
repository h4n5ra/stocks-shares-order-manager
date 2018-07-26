package OrderManager;

import java.io.Serializable;

/*
	Fill represents a fill of an order, i.e. a part that has been satisfied.
 */
public class Fill implements Serializable {
	//long id;
	protected int size;
 	protected double price;
	Fill(int size,double price){
		this.size=size;
		this.price=price;
	}
}
