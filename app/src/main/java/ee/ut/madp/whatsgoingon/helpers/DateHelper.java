package ee.ut.madp.whatsgoingon.helpers;

import android.content.Context;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ee.ut.madp.whatsgoingon.R;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.DATE_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.DATE_TIME_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.FULL_DATE_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.FULL_DATE_TIME_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.TIME_FORMAT;

/**
 * Created by admin on 27.10.2017.
 */

public class DateHelper {

    public static DateTime parseDateFromString(String stringDate) {
        DateTime parseDate = null;
        if (!stringDate.isEmpty()) {
            String pattern = stringDate.matches("[a-zA-Z]{3} \\s \\d{1,2} \\s \\d{4}") ? FULL_DATE_FORMAT : DATE_FORMAT;
            DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
            parseDate = formatter.parseDateTime(stringDate);
            if (!pattern.contains("yyyy")) {
                parseDate = parseDate.withYear(LocalDate.now().getYear());
            }
        }
        return parseDate;

    }

    /**
     * Creates message timestamp. For today AT 5:20 pm, for yesterday YESTERDAY AT 5:20 otherwise date
     *
     * @param time
     * @return
     */
    public static String createMessageTime(Context context, long time) {
        DateTimeFormatter dateTimeFormatter;
        String messageTime = "";

        boolean isToday = isToday(time);
        boolean isYesterday =  LocalDate.now().minusDays(1).compareTo(new LocalDate(time)) == 0;
        boolean isSameYear = isSameYear(time);

        if (isToday) {
            dateTimeFormatter = DateTimeFormat.forPattern(TIME_FORMAT);
            messageTime = dateTimeFormatter.print(time);
        } else if (isYesterday) {
            dateTimeFormatter = DateTimeFormat.forPattern(TIME_FORMAT);
            messageTime = context.getString(R.string.yesterday_sent) + " " + dateTimeFormatter.print(time);
        } else if (isSameYear) {
            dateTimeFormatter = DateTimeFormat.forPattern(DATE_TIME_FORMAT);
            messageTime = dateTimeFormatter.print(time);
        } else {
            dateTimeFormatter = DateTimeFormat.forPattern(FULL_DATE_TIME_FORMAT);
            messageTime = dateTimeFormatter.print(time);
        }

        return messageTime;
    }

    public static boolean isToday(long time) {
        return LocalDate.now().compareTo(new LocalDate(time)) == 0;
    }

    public static boolean isSameYear(long time) {
        return new DateTime(time).getYear() - LocalDate.now().getYear() == 0;
    }

    public static boolean isPast(long time) {
        return !isToday(time) && new DateTime(time).isBeforeNow();
    }

    public static boolean isFutureTime(long time) {
        return new DateTime(time).isAfter(DateTime.now());
    }


}
