package ee.ut.madp.whatsgoingon.chat;

/**
 * Interface for notification system. Class implementing this interface keeps track of observers
 * and send notifications to them in case of event.
 *
 * Created by dominikf on 25. 10. 2017.
 */

public interface Observable {

    void addObserver(Observer obs);

    void deleteObserver(Observer obs);
}
