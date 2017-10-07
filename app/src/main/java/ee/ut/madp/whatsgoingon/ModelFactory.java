package ee.ut.madp.whatsgoingon;

import ee.ut.madp.whatsgoingon.models.User;

public class ModelFactory {
    /**
     * Creates a user by given properties
     * @param id
     * @param photo
     * @param email
     * @param name
     * @return User
     */
    public static User createUser(String id, String photo, String email, String name) {
        return new User(id, photo, email, name);
    }
}
