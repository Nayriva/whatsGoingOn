package ee.ut.madp.whatsgoingon;

import java.util.Comparator;

import ee.ut.madp.whatsgoingon.models.ChatChannel;

/**
 * Created by admin on 31.10.2017.
 */
public class ChatChannelComparator implements Comparator<ChatChannel> {

    @Override
    public int compare(ChatChannel o1, ChatChannel o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
