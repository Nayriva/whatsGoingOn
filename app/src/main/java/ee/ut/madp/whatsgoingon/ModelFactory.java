package ee.ut.madp.whatsgoingon;

import java.util.Date;

import ee.ut.madp.whatsgoingon.models.Event;
import ee.ut.madp.whatsgoingon.models.User;

public class ModelFactory {

    public static User createUser(String id, String photo, String email, String name) {
        return new User(id, photo, email, name);
    }

    public static User createUserProfiled(String id, String name, String email, String photo,
                                          String nationality, String city, String phoneNumber, String school,
                                          String work, Date birthday) {
        return new User(id, name, email, photo, nationality, city, phoneNumber, school, work, birthday);
    }

    public static Event createNewEvent(String id, String name, String place, String description, long date, String owner, long dateTime) {
        return new Event(id, name, place, description, date, owner, dateTime);
    }
}
