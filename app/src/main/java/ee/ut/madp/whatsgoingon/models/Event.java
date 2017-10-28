package ee.ut.madp.whatsgoingon.models;

/**
 * Created by admin on 28.10.2017.
 */

public class Event {
    private String id, name, description;
    private long dateTime;

    public Event() {
    }

    public Event(String id, String name, String description, long dateTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dateTime = dateTime;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }
}
