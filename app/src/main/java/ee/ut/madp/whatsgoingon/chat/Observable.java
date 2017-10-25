package ee.ut.madp.whatsgoingon.chat;

/**
 * Created by dominikf on 25. 10. 2017.
 */

public interface Observable {

    void addObserver(Observer obs);

    void deleteObserver(Observer obs);
}
