package ee.ut.madp.whatsgoingon.models;

import java.util.List;

/**
 * Model of group as a chat channel in system
 *
 * Created by dominikf on 21. 10. 2017.
 */

public class Group {
    private String id;
    private String displayName;
    private String photo;
    private List<String> receivers;

    public Group(String id, String displayName, String photo, List<String> receivers) {
        this.id = id;
        this.displayName = displayName;
        this.receivers = receivers;
        this.photo = photo;
    }

    public Group() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public List<String> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<String> receivers) {
        this.receivers = receivers;
    }
}
