package ee.ut.madp.whatsgoingon.chat;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;

/**
 * Created by dominikf on 3. 10. 2017.
 */

public class ChatChannelAdapter extends ArrayAdapter<ChatChannel> {
    private Context context;
    private int layoutResourceId;
    private ArrayList<ChatChannel> data = null;

    public ChatChannelAdapter(Context context, int resource, List<ChatChannel> objects) {
        super(context, resource, objects);
        this.layoutResourceId = resource;
        this.context = context;
        this.data = (ArrayList) objects;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        ChatChannelHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ChatChannelHolder();
            holder.photo = (CircleImageView) row.findViewById(R.id.iv_user_photo);
            holder.channelName = (TextView) row.findViewById(R.id.tv_user_name);
            holder.lastMessage = (TextView) row.findViewById(R.id.tv_last_chat);
            holder.messageTime = (TextView) row.findViewById(R.id.tv_time);
            holder.onlineIndicator =  row.findViewById(R.id.online_indicator);
            //holder.newMessage = (RelativeLayout) row.findViewById(R.id.rl_chat_channel) ;

            row.setTag(holder);
        } else {
            holder = (ChatChannelHolder) row.getTag();
        }

        ChatChannel item = data.get(position);
        String photo = item.getPhoto();
        if (photo != null) {
            if (photo.contains("http")) {
                Picasso.with(getContext()).load(photo).into(holder.photo);
            } else {
                holder.photo.setImageBitmap(ImageHelper.decodeBitmap(photo));
            }
        }
        holder.channelName.setText(item.getName());
        holder.lastMessage.setText(item.getLastMessage());
        holder.messageTime.setText(item.getTimeMessage());
//        if (item.isNewMessage()) {
//            holder.newMessage.setBackgroundColor(getContext().getResources().getColor(R.color.colorNewMessage));
//        } else {
//            holder.newMessage.setBackgroundColor(0x00000000);
//        }
        return row;
    }

    private class ChatChannelHolder {
        CircleImageView photo;
        TextView channelName;
        TextView lastMessage;
        TextView messageTime;
        View onlineIndicator;

        //RelativeLayout newMessage;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    public List<ChatChannel> getItems() {
        return data;
    }
}
