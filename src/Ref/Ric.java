package Ref;

import java.io.Serializable;

//All this does is initialize an RIC. The split makes it so that the company and the exchange can be acquired as
//they will be either side of the '.' which is used as a delimiter.
public class Ric implements Serializable{
	protected String ric;
	public Ric(String ric){
		this.ric=ric;
	}
	public String getEx() {
		return ric.split(".")[1];
	}
	public String getCompany(){
		return ric.split(".")[0];
	}
	public String toString() {
		return ric;
	}
}