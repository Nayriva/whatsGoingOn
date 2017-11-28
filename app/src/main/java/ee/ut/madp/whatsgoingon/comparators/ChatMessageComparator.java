package ee.ut.madp.whatsgoingon.comparators;

import java.util.Comparator;

import ee.ut.madp.whatsgoingon.models.ChatMessage;

/**
 * Created by dominikf on 26. 11. 2017.
 */

public class ChatMessageComparator implements Comparator<ChatMessage> {

    @Override
    public int compare(ChatMessage message, ChatMessage t1) {
        if (message == null && t1 == null) {
            return 0;
        } else if (message == null) {
            return 1;
        } else if (t1 == null) {
            return -1;
        } else {
            return (int) (message.getMessageTime() - t1.getMessageTime());
        }
    }
}
