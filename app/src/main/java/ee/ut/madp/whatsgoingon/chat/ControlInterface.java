package ee.ut.madp.whatsgoingon.chat;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

/**
 * Interface for sending control messages via AllJoyn bus object.
 *
 * Created by dominikf on 31. 10. 2017.
 */

@BusInterface(name = "ee.ut.madp.whatisgoingon.control")
public interface ControlInterface {

    @BusSignal
    void control(String message) throws BusException;
}
