package ee.ut.madp.whatsgoingon.comparators;

import java.util.Comparator;

import ee.ut.madp.whatsgoingon.models.Event;

/**
 * Created by dominikf on 9. 11. 2017.
 */

public class EventComparator implements Comparator<Event> {

    @Override
    public int compare(Event event, Event t1) {
        if (event == null && t1 == null) {
            return 0;
        } else if (event == null) {
            return 1;
        } else if (t1 == null) {
            return -1;
        }
        long eventDateTime = event.getDateTime();
        long t1DateTime = t1.getDateTime();
        return (int) (eventDateTime - t1DateTime);
    }
}
