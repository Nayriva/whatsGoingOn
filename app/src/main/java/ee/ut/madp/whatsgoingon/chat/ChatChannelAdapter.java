package ee.ut.madp.whatsgoingon.chat;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
            holder.photo = (ImageView) row.findViewById(R.id.iw_chat_channel_picture);
            holder.channelName = (TextView) row.findViewById(R.id.tw_chat_channel_name);

            row.setTag(holder);
        } else {
            holder = (ChatChannelHolder) row.getTag();
        }

        ChatChannel item = data.get(position);
        String photo = item.getPhoto();
        if (photo.contains("http")) {
            Picasso.with(getContext()).load(photo).into(holder.photo);
        } else {
            holder.photo.setImageBitmap(ImageHelper.decodeBitmap(photo));
        }
        holder.channelName.setText(item.getName());

        return row;
    }

    private class ChatChannelHolder {
        ImageView photo;
        TextView channelName;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }
}
