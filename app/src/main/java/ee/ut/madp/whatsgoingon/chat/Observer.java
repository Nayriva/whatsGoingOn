package ee.ut.madp.whatsgoingon.chat;

/**
 * Interface for notification system. Class implementing this interface can by notified by Observable
 * of event happened.
 *
 * Created by dominikf on 25. 10. 2017.
 */

public interface Observer {

    void update(Observable o, int qualifier, String data);
}
