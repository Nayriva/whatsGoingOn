package ee.ut.madp.whatsgoingon.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Class represents event created and managed by user.
 *
 * Created by admin on 28.10.2017.
 */

public class Event implements Parcelable {
    //date for filtering
    private String id, name, description, owner, place, googleEventId;
    // id in calendar
    private long eventId;
    private long dateTime, date;
    private List<String> attendantIds;

    @Exclude
    private boolean isJoined;

    public Event() { // required constructor
    }

    public Event(String id, String name, String place, String description,
                 long date, String owner, long dateTime) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.place = place;
        this.description = description;
        this.dateTime = dateTime;
        this.owner = owner;
        attendantIds = new ArrayList<>();
        attendantIds.add(owner);
    }

    protected Event(Parcel in) {
        id = in.readString();
        eventId = in.readLong();
        googleEventId = in.readString();
        name = in.readString();
        place = in.readString();
        description = in.readString();
        owner = in.readString();
        dateTime = in.readLong();
        date = in.readLong();
        isJoined = in.readByte() != 0;
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

    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
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

    public boolean isJoined() {
        return isJoined;
    }

    public void setJoined(boolean joined) {
        isJoined = joined;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeLong(eventId);
        dest.writeString(googleEventId);
        dest.writeString(name);
        dest.writeString(place);
        dest.writeString(description);
        dest.writeString(owner);
        dest.writeLong(dateTime);
        dest.writeLong(date);
        dest.writeByte((byte) (isJoined ? 1 : 0));
    }
}
