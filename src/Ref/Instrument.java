package Ref;

import java.io.Serializable;
import java.util.Date;

//Constructor just initialises a RIC for the instrument; all of these other member variables are never used.
//Subclasses need filling out, but they're never actually used.
//TODO: getters and setters for each member variable? Also, member variables could be private.
public class Instrument implements Serializable{

	protected Ric ric;
	protected long id;
	protected String name;
	protected String isin;
	protected String sedol;
	protected String bbid;

	public Instrument(Ric ric){
		this.ric=ric;
	}
	public String toString(){
		return ric.ric;
	}
}
class EqInstrument extends Instrument{
	protected Date exDividend;

	public EqInstrument(Ric ric){
		super(ric);
	}
}
class FutInstrument extends Instrument{
	protected Date expiry;
	protected Instrument underlier;

	public FutInstrument(Ric ric){
		super(ric);
	}
}
/*TODO
Index
bond
methods
*/