package ee.ut.madp.whatsgoingon.comparators;

import org.joda.time.DateTime;

import java.util.Comparator;

import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;

/**
 * Comparator used for sorting of ChatChannels. Sorting is based on how old the last message is.
 *
 * Created by dominikf on 9. 11. 2017.
 */

public class ChatChannelComparator implements Comparator<ChatChannel> {

    @Override
    public int compare(ChatChannel chatChannel, ChatChannel chatChannel2) {
        if (chatChannel == null && chatChannel2 == null) {
            return 0;
        } else if (chatChannel == null) {
            return -1;
        } else if (chatChannel2 == null) {
            return 1;
        }

        String t1 = chatChannel.getLastMessageTime();
        String t2 = chatChannel2.getLastMessageTime();
        if (t1 == null && t2 == null) {
            return 0;
        } else if (t1 == null) {
            return 1;
        } else if (t2 == null) {
            return -1;
        } else {
            DateTime t1Date = DateHelper.parseTimeFromString(t1);
            DateTime t2Date = DateHelper.parseTimeFromString(t2);
            return -1 * t1Date.compareTo(t2Date);
        }
    }
}
