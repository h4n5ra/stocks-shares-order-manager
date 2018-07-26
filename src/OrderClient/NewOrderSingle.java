package OrderClient;

import java.io.Serializable;

import Ref.Instrument;
import Tools.FixTagRef;

/* Serializable is a Java class. All this class is, is a template for individual orders that implements the Serializable
interface. It doesn't implement any methods from this but implementing it makes it serializable. All it does is it
contains a size for the order, a price and an instrument; and a call to the constructor to initializes the new order
object to contain all of these. */

public class NewOrderSingle implements Serializable {
	private int size;
	private int instrid;
	private char status=FixTagRef.PENDING_NEW;
	private Instrument instrument;
	private long OMOrderID;

	public char getStatus() {
		return status;
	}

	public void setStatus(char status) {
		this.status = status;
	}

	public long getOMOrderID() {
		return OMOrderID;
	}

	public void setOMOrderID(long OMOrderID) {
		this.OMOrderID = OMOrderID;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public NewOrderSingle(int size,int instrid,Instrument instrument){
		this.size=size;
		this.instrid=instrid;
		this.instrument=instrument;
	}
}