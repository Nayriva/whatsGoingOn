package ee.ut.madp.whatsgoingon;

import ee.ut.madp.whatsgoingon.models.Event;
import ee.ut.madp.whatsgoingon.models.User;

public class ModelFactory {

    public static User createUser(String id, String photo, String email, String name) {
        return new User(id, photo, email, name);
    }

    public static Event createNewEvent(String id, String name, String place, String description,
                                       long date, String owner, long dateTime) {
        return new Event(id, name, place, description, date, owner, dateTime);
    }
}
