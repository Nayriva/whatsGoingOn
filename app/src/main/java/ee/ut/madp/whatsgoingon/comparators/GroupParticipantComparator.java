package ee.ut.madp.whatsgoingon.comparators;

import java.util.Comparator;

import ee.ut.madp.whatsgoingon.models.GroupParticipant;

/**
 * Created by dominikf on 9. 11. 2017.
 */

public class GroupParticipantComparator implements Comparator<GroupParticipant> {
    @Override
    public int compare(GroupParticipant groupParticipant, GroupParticipant t1) {
        return groupParticipant.getName().compareTo(t1.getName());
    }
}
