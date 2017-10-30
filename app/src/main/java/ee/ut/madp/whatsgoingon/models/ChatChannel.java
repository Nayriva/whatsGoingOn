
package ee.ut.madp.whatsgoingon.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by dominikf on 21. 10. 2017.
 */
public class ChatChannel implements Parcelable {

    private String id;
    private String name;
    private String photo;
    private boolean newMessage = false;
    private String lastMessage;
    private String timeMessage;
    private boolean isOnline;
    private boolean isGroup;
    private String[] receivers;

    public ChatChannel(String id, String name, String photo, boolean isGroup) {
        this.id = id;
        this.name = name;
        this.photo = photo;
        this.isGroup = isGroup;
    }

    protected ChatChannel(Parcel in) {
        id = in.readString();
        name = in.readString();
        if (photo != null) photo = in.readString();
        if (receivers != null) receivers = in.createStringArray();
        newMessage = in.readByte() != 0;
        isOnline = in.readByte() != 0;
        isGroup = in.readByte() != 0;
    }

    public static final Creator<ChatChannel> CREATOR = new Creator<ChatChannel>() {
        @Override
        public ChatChannel createFromParcel(Parcel in) {
            return new ChatChannel(in);
        }

        @Override
        public ChatChannel[] newArray(int size) {
            return new ChatChannel[size];
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

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public boolean isNewMessage() {
        return newMessage;
    }

    public void setNewMessage(boolean newMessage) {
        this.newMessage = newMessage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getTimeMessage() {
        return timeMessage;
    }

    public void setTimeMessage(String timeMessage) {
        this.timeMessage = timeMessage;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String[] getReceivers() {
        return receivers;
    }

    public void setReceivers(String[] receivers) {
        this.receivers = receivers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        if (photo != null) dest.writeString(photo);
        if (receivers != null) dest.writeArray(receivers);
        dest.writeByte((byte) (newMessage ? 1 : 0));
        dest.writeByte((byte) (isOnline ? 1 : 0));
        dest.writeByte((byte) (isGroup ? 1 : 0));
    }
}
