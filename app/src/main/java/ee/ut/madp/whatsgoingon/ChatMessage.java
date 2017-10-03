package ee.ut.madp.whatsgoingon;

import java.util.Date;

/**
 * Created by dominikf on 3. 10. 2017.
 */

public class ChatMessage {

    private String messageText;
    private String messageAuthor;
    private long messageTime;

    public ChatMessage(String messageText, String messageAuthor) {
        this.messageText = messageText;
        this.messageAuthor = messageAuthor;
        this.messageTime = new Date().getTime();
    }

    public ChatMessage() {
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageAuthor() {
        return messageAuthor;
    }

    public void setMessageAuthor(String messageAuthor) {
        this.messageAuthor = messageAuthor;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }
}
