package Tools;

import Ref.Ric;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class FixTag implements IFixTag {

    private static final char ERRC = '\0';
    private static final long ERRL = -1;
    private static final Ric ERRRIC = new Ric("NAN.NAN");

    char msgType;
    private static final char newOrderSingle = 'D';
    private static final char cancelOrderRequest = 'F';
    private static final char executionReport = '8';

    long OMOrderId;
    long COrderId;
    char ordStatus;
    char side;
    int quantity;
    Ric ric;
    public char getMsgType() {
        return msgType;
    }

    public long getOMOrderId() {
        return OMOrderId;
    }

    public long getCOrderId() {
        return COrderId;
    }

    public char getOrdStatus() {
        return ordStatus;
    }

    public char getSide() {
        return side;
    }

    public int getQuantity() {
        return quantity;
    }

    public Ric getRic() {
        return ric;
    }


    FixTag() {
        msgType = ERRC;
        OMOrderId=ERRL;
        COrderId=ERRL;
        ordStatus=ERRC;
        side = ERRC;
        quantity = (int)ERRL;
        ric = ERRRIC;
    }

    public void send(ObjectOutputStream stream) throws IOException {
        String fix=print();
        stream.writeObject(fix);
        stream.flush();
    }

    public String print() {
        String fix="";
        switch(msgType) {
            case newOrderSingle:
                fix = "35=D;11=" + COrderId + ";55=" + ric.toString() + ";54=" + side + ";38=" + quantity;
                break;
            case executionReport:
                fix = "35=8;37=" + OMOrderId + ";11=" + COrderId + ";39=" + ordStatus;//TODO add quantities
                break;
            case cancelOrderRequest:
                fix = "35=F;37=" + OMOrderId + ";11=" + COrderId;//TODO add quantities
                break;
            //TODO
            default:

        }
        return fix;
    }
}
