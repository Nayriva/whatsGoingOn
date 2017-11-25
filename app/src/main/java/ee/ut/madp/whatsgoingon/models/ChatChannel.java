
package ee.ut.madp.whatsgoingon.models;

/**
 * Model of chat channel.
 *
 * Created by dominikf on 21. 10. 2017.
 */
public class ChatChannel {

    private String id;
    private String name;
    private String photo;
    private boolean newMessage = false;
    private String lastMessageText;
    private String lastMessageTime;
    private boolean isGroup;

    public ChatChannel(String id, String name, String photo, boolean isGroup) {
        this.id = id;
        this.name = name;
        this.photo = photo;
        this.isGroup = isGroup;
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

    public String getLastMessage() {
        return lastMessageText;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessageText = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }
}
