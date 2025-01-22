import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {
    public static synchronized String dateToString(Date date){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return dateFormat.format(date);
    }

    public static synchronized Date stringToDate(String dateString) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(dateString);
    }
}
