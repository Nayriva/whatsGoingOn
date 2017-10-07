package ee.ut.madp.whatsgoingon.models;

/**
 * Created by admin on 07.10.2017.
 */

public class User {
    private String id;
    private String name;
    private String email;
    private String photo;

    public User() {
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User(String id, String photo, String email, String name) {
        this.id = id;
        this.photo = photo;
        this.email = email;
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", photo='" + photo + '\'' +
                '}';
    }
}