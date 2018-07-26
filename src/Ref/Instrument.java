package Ref;

import java.io.Serializable;
import java.util.Date;

//Constructor just initialises a RIC for the instrument; all of these other member variables are never used.
//Subclasses need filling out, but they're never actually used.
public class Instrument implements Serializable{

	protected Ric ric;
	protected long id;

	public Instrument(Ric ric){
		this.ric=ric;
	}
	public String toString(){
		return ric.ric;
	}
}