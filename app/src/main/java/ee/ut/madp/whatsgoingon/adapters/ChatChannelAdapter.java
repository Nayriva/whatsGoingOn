package ee.ut.madp.whatsgoingon.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.NewEventActivity;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;

/**
 * Created by admin on 27.10.2017.
 */

public class ChatChannelAdapter extends RecyclerView.Adapter<ChatChannelAdapter.MyViewHolder> {
    private List<ChatChannel> channelList;
    private Context context;

    public ChatChannelAdapter(Context context, List<ChatChannel> channelList) {
        this.channelList = channelList;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_chat_channels, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        ChatChannel chatChannel = channelList.get(position);
        String photo = chatChannel.getPhoto();
        if (photo != null) {
            if (photo.contains("http")) {
                Picasso.with(context).load(photo).into(holder.photo);
            } else {
                holder.photo.setImageBitmap(ImageHelper.decodeBitmap(photo));
            }
        }
        holder.channelName.setText(chatChannel.getName());
        holder.lastMessage.setText(chatChannel.getLastMessage());
        holder.messageTime.setText(chatChannel.getTimeMessage());
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        CircleImageView photo;
        TextView channelName;
        TextView lastMessage;
        TextView messageTime;
        View onlineIndicator;

        public MyViewHolder(View view) {
            super(view);
            photo = (CircleImageView) view.findViewById(R.id.iv_user_photo);
            channelName = (TextView) view.findViewById(R.id.tv_user_name);
            lastMessage = (TextView) view.findViewById(R.id.tv_last_chat);
            messageTime = (TextView) view.findViewById(R.id.tv_time);
            onlineIndicator =  view.findViewById(R.id.online_indicator);
        }

        @Override
        public void onClick(View v) {
            ChatChannel contact = channelList.get(getAdapterPosition());
            context.startActivity(new Intent(v.getContext(), NewEventActivity.class));
        }
    }
}
