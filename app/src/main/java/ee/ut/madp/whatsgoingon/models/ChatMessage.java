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
    private String displayName;
    private String sender;
    private long messageTime;
    private boolean isMe;

    public ChatMessage(String messageText, String displayName, String sender, boolean isMe) {
        this.messageText = messageText;
        this.displayName = displayName;
        this.sender = sender;
        this.messageTime = new Date().getTime();
        this.isMe = isMe;
    }

    public ChatMessage(String messageText, String displayName, String sender, boolean isMe, long time) {
        this.messageText = messageText;
        this.displayName = displayName;
        this.sender = sender;
        this.messageTime = time;
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

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
