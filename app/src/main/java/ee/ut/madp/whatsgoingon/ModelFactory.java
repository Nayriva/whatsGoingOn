package ee.ut.madp.whatsgoingon;

import java.util.Date;

import ee.ut.madp.whatsgoingon.models.User;

public class ModelFactory {

    public static User createUser(String id, String photo, String email, String name) {
        return new User(id, photo, email, name);
    }

    public static User createUserProfiled(String id, String name, String email, String photo,
                                          String nationality, String phoneNumber, String school,
                                          String work, Date birthday) {
        return  new User(id, name, email, photo, nationality, phoneNumber, school, work, birthday);
    }
}
