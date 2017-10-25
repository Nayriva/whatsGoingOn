package ee.ut.madp.whatsgoingon.models;

/**
 * Created by dominikf on 21. 10. 2017.
 */

public class ChatChannel {
    private String id;
    private String name;
    private String photo;
    private boolean newMessage = false;

    public ChatChannel(String id, String name, String photo) {
        this.id = id;
        this.name = name;
        this.photo = photo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public boolean isNewMessage() {
        return newMessage;
    }

    public void setNewMessage(boolean newMessage) {
        this.newMessage = newMessage;
    }
}
