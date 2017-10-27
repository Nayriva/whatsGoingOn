package ee.ut.madp.whatsgoingon.helpers;

import android.content.Context;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ee.ut.madp.whatsgoingon.R;

/**
 * Created by admin on 27.10.2017.
 */

public class DateHelper {

    private static final String FULL_DATE_TIME_FORMAT = "MMM dd, yyyy HH:mm";
    private static final String TIME_FORMAT = "HH:mm";
    private static final String DATE_TIME = "MMM dd HH:mm";

    private static DateTimeFormatter dateTimeFormatter;
    
    /**
     * Creates message timestamp. For today AT 5:20 pm, for yesterday YESTERDAY AT 5:20 otherwise date
     *
     * @param time
     * @return
     */
    public static String createMessageTime(Context context, long time) {
        String messageTime = "";
        DateTime jodaTime =  new DateTime(Long.valueOf(time), DateTimeZone.UTC);;

        boolean isToday =  LocalDate.now().compareTo(new LocalDate(time)) == 0;
        boolean isYesterday =  LocalDate.now().minusDays(1).compareTo(new LocalDate(time)) == 0;
        boolean isSameYear = jodaTime.getYear() - LocalDate.now().getYear() == 0;

        if (isToday) {
            dateTimeFormatter = DateTimeFormat.forPattern(TIME_FORMAT);
            messageTime = dateTimeFormatter.print(time);
        } else if (isYesterday) {
            dateTimeFormatter = DateTimeFormat.forPattern(TIME_FORMAT);
            messageTime = context.getString(R.string.yesterday_sent) + " " + dateTimeFormatter.print(time);
        } else if (isSameYear) {
            dateTimeFormatter = DateTimeFormat.forPattern(DATE_TIME);
            messageTime = dateTimeFormatter.print(time);
        } else {
            dateTimeFormatter = DateTimeFormat.forPattern(FULL_DATE_TIME_FORMAT);
            messageTime = dateTimeFormatter.print(time);
        }

        return messageTime;
    }


}
