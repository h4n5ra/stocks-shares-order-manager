package Tools;

import org.apache.log4j.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


public class MyLogger {
    private static HashMap<String, Logger> loggerList = new HashMap<>();
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private static Date date;
    public static void init (){
        PropertyConfigurator.configure("resources/log4j.properties");
    }

    private static Logger getLogger(String id){
        if(loggerList.containsKey(id))
            return loggerList.get(id);
        else {
            loggerList.put(id, Logger.getLogger(id));
            return loggerList.get(id);
        }
    }

    private static String getClassName() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String part = stackTraceElements[3].toString();
        String[] parts = part.split("\\.");
        return parts[0];
    }

    public static void out (String message){
        Logger logger = getLogger(getClassName());
        logger.log(Level.INFO, message);
        System.out.println(getClassName() + ": " + message);
    }

    public static void out (String message, Level logLevel){
        Logger logger = getLogger(getClassName());
        logger.log(logLevel, message);
        if(logLevel == Level.WARN || logLevel == Level.ERROR || logLevel == Level.FATAL)
            System.err.println(getClassName() + ": " + message);
        else
            System.out.println(getClassName() + ": " + message);
    }
}
