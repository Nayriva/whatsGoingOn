package ee.ut.madp.whatsgoingon.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by admin on 28.10.2017.
 */

public class Event implements Parcelable{
    //date for filtering
    private String id, name, description, owner, place;
    private long dateTime, date;
    private List<String> attendantIds;

    public Event() {
    }

    public Event(String id, String name, String place, String description, long date, String owner, long dateTime) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.place = place;
        this.description = description;
        this.dateTime = dateTime;
        this.owner = owner;
    }

    protected Event(Parcel in) {
        id = in.readString();
        name = in.readString();
        place = in.readString();
        description = in.readString();
        owner = in.readString();
        dateTime = in.readLong();
        date = in.readLong();
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public List<String> getAttendantIds() {
        return attendantIds;
    }

    public void setAttendantIds(List<String> attendantIds) {
        this.attendantIds = attendantIds;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(place);
        dest.writeString(description);
        dest.writeString(owner);
        dest.writeLong(dateTime);
        dest.writeLong(date);
    }
}
