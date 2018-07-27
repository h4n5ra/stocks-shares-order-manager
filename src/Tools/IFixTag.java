package Tools;

import Ref.Ric;

import java.io.IOException;
import java.io.ObjectOutputStream;

public interface IFixTag {

    void send(ObjectOutputStream stream) throws IOException ;
    String print();

    char getMsgType();

    int getOMOrderId();

    int getCOrderId();

    char getOrdStatus();

    char getSide();

    int getQuantity();

    Ric getRic();
}
