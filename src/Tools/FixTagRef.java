package Tools;

public class FixTagRef {
    //message types
    public static final char NEW_ORDER_SINGLE = 'D';
    public static final char EXECUTION_REPORT = '8';
    public static final char CANCEL_REQUEST = 'F';


    //order statuses
    public static final char PENDING_NEW = 'A';
    public static final char NEW= '0';
    public static final char PARTIAL_FILL = '1';
    public static final char FILLED = '2';
    public static final char PENDING_CANCELLED = '6';
    public static final char CANCELLED = '4';

    //sides
    public static final char BUY = '1';
    public static final char SELL = '2';
}
