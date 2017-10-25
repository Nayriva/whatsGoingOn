package ee.ut.madp.whatsgoingon.chat;

/**
 * Created by dominikf on 25. 10. 2017.
 */

public interface Observer {

    void update(Observable o, String qualifier, String data);
}
