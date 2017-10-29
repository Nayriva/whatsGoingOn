package ee.ut.madp.whatsgoingon;

import org.joda.time.DateTime;

import java.util.Comparator;

import ee.ut.madp.whatsgoingon.models.Event;

/**
 * Created by admin on 29.10.2017.
 */

public class TimeComparator implements Comparator<Event> {


    @Override
    public int compare(Event o1, Event o2) {
        return new DateTime(o1.getDateTime()).compareTo(new DateTime(o2.getDateTime()));
    }
}
