package ee.ut.madp.whatsgoingon.chat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

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
            holder.photo = (ImageView) row.findViewById(R.id.iw_chat_channel_picture);
            holder.channelName = (TextView) row.findViewById(R.id.tv_chat_channel_name);
            holder.newMessage = (RelativeLayout) row.findViewById(R.id.rl_chat_channel) ;

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
        if (item.isNewMessage()) {
            holder.newMessage.setBackgroundColor(getContext().getResources().getColor(R.color.colorNewMessage));
        } else {
            holder.newMessage.setBackgroundColor(0x00000000);
        }
        return row;
    }

    private class ChatChannelHolder {
        ImageView photo;
        TextView channelName;
        RelativeLayout newMessage;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    public List<ChatChannel> getItems() {
        return data;
    }
}
