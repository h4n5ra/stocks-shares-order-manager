package Tools;

import Ref.Ric;

import java.io.IOException;
import java.io.ObjectInputStream;

public class FixTagFactory {

    public static IFixTag makeNewOrderSingle(int clientOrderID, String RIC, char side, int quantity) {
        FixTag f = new FixTag();
        f.msgType=FixTagRef.NEW_ORDER_SINGLE;
        f.COrderId=clientOrderID;
        f.ric = new Ric(RIC);
        f.side = side;
        f.quantity = quantity;
        return f;
    }

    public static IFixTag makeExecutionReport(long orderManagerOrderID, long clientOrderID, char status) {
        FixTag f = new FixTag();
        f.msgType=FixTagRef.EXECUTION_REPORT;
        f.OMOrderId=orderManagerOrderID;
        f.COrderId=clientOrderID;
        f.ordStatus = status;
        return f;
    }

    public static IFixTag makeCancelRequest(long clientOrderID, long orderManagerOrderID) {
        FixTag f = new FixTag();
        f.msgType=FixTagRef.CANCEL_REQUEST;
        f.COrderId=clientOrderID;
        f.OMOrderId=orderManagerOrderID;
        return f;
    }

    public static IFixTag makeCancelConfirmation(long clientOrderID, long orderManagerOrderID) {
        FixTag f = new FixTag();
        f.msgType=FixTagRef.EXECUTION_REPORT;
        f.OMOrderId=orderManagerOrderID;
        f.COrderId=clientOrderID;
        f.ordStatus = FixTagRef.CANCELLED;
        return f;
    }

    public static IFixTag read(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        FixTag result = new FixTag();
        String fix=(String)stream.readObject();
        String[] fixTags=fix.split(";");
        for(int i=0;i<fixTags.length;i++){
            String[] tag_value=fixTags[i].split("=");
            switch(tag_value[0]){
                case"35":
                    result.msgType=tag_value[1].charAt(0);
                    break;
                case"11":
                    result.COrderId=Long.parseLong(tag_value[1]);
                    break;
                case"37":
                    result.OMOrderId=Long.parseLong(tag_value[1]);
                    break;
                case"39":
                    result.ordStatus=tag_value[1].charAt(0);
                    break;
                case"54":
                    result.side=tag_value[1].charAt(0);
                    break;
                case"38":
                    result.quantity=Integer.parseInt(tag_value[1]);
                    break;
                case"55":
                    result.ric=new Ric(tag_value[1]);
                    break;
                case"40":
                    result.type=tag_value[1].charAt(0);
                    break;
            }
        }
        return result;
    }
}
