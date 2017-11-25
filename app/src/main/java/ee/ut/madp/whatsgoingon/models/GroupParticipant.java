package ee.ut.madp.whatsgoingon.models;

/**
 * Model of participant of group.
 *
 * Created by dominikf on 21. 10. 2017.
 */

public class GroupParticipant {
    private String id;
    private String name;
    private String photo;
    private boolean selected;

    public GroupParticipant(String id, String name, String photo, boolean selected) {
        this.id = id;
        this.name = name;
        this.photo = photo;
        this.selected = selected;
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

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
