package ee.ut.madp.whatsgoingon.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by admin on 27.10.2017.
 */

public class DateHelper {

    public static String convertTimeToString(long time) {
        Date date = new Date(time);
        SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");
        String dateText = df2.format(date);
        return dateText;
    }
}
