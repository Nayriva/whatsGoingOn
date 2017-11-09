package ee.ut.madp.whatsgoingon.chat;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

/**
 * Created by dominikf on 31. 10. 2017.
 */

@BusInterface(name = "ee.ut.madp.whatisgoingon.control")
public interface ControlInterface {

    @BusSignal
    void Control(String message) throws BusException;
}
