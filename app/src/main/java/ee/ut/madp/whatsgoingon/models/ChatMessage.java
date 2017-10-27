package ee.ut.madp.whatsgoingon.models;


import java.util.Date;

/**
 * Class represents message sent in chat. Every message has it's author, time when it was sent
 * and the text of the message.
 *
 * Created by dominikf on 3. 10. 2017.
 */
public class ChatMessage {

    private String messageText;

    private String authorPhoto;
    private long messageTime;
    private boolean isMe;

    public ChatMessage(String messageText, boolean isMe) {
        this.messageText = messageText;
        this.messageTime = new Date().getTime();
        this.isMe = isMe;
    }

    public ChatMessage() {
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getAuthorPhoto() {
        return authorPhoto;
    }

    public void setAuthorPhoto(String authorPhoto) {
        this.authorPhoto = authorPhoto;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }
}
