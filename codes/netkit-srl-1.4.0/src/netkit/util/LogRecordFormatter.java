package netkit.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogRecordFormatter extends Formatter {
  private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  @Override
  public String format(LogRecord record) {
    Level level = record.getLevel();
    String msg = record.getMessage();
    Long millis = record.getMillis();
    Date dt = new Date(millis);
    // String callingClass = record.getSourceClassName();
    // String callingMethod = record.getSourceMethodName();
    
    return df.format(dt)+" ["+level+"] "+msg+NetKitEnv.newline;
  }

}
