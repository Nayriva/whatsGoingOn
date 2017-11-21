package ee.ut.madp.whatsgoingon.adapters;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.ConversationActivity;
import ee.ut.madp.whatsgoingon.activities.UserProfileActivity;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.CHANNEL_ID;

/**
 * Created by admin on 27.10.2017.
 */

public class ChatChannelAdapter extends RecyclerView.Adapter<ChatChannelAdapter.ChatChannelViewHolder> {

    private List<ChatChannel> channelList;

    public ChatChannelAdapter(List<ChatChannel> channelList) {
        this.channelList = channelList;
    }

    @Override
    public ChatChannelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_chat_channels, parent, false);

        return new ChatChannelViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ChatChannelViewHolder holder, int position) {
        ChatChannel chatChannel = channelList.get(position);
        String photo = chatChannel.getPhoto();

        holder.photo.setImageBitmap(ImageHelper.decodeBitmap(photo));
        holder.channelName.setText(chatChannel.getName());
        if (chatChannel.isNewMessage()) {
            holder.lastMessage.setTypeface(holder.lastMessage.getTypeface(), Typeface.BOLD);
            holder.messageTime.setTypeface(holder.messageTime.getTypeface(), Typeface.BOLD);
        } else {
            holder.lastMessage.setTypeface(holder.lastMessage.getTypeface(), Typeface.NORMAL);
            holder.messageTime.setTypeface(holder.messageTime.getTypeface(), Typeface.NORMAL);
        }
        holder.lastMessage.setText(chatChannel.getLastMessage());
        holder.messageTime.setText(chatChannel.getLastMessageTime());
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    public void addChannel(ChatChannel foundChannel) {
        channelList.add(foundChannel);
    }

    public void removeChannel(ChatChannel channel) {
        channelList.remove(channel);
    }

    public ChatChannel getChannelById(String id) {
        for (ChatChannel chatChannel: channelList) {
            if (chatChannel.getId().equals(id)) {
                return chatChannel;
            }
        }
        return null;
    }

    public List<ChatChannel> getChannels() {
        return channelList;
    }

    public void clearChannels() {
        channelList.clear();
    }

    public class ChatChannelViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CircleImageView photo;
        TextView channelName;
        TextView lastMessage;
        TextView messageTime;

        public ChatChannelViewHolder(View view) {
            super(view);
            photo = (CircleImageView) view.findViewById(R.id.iv_user_photo);
            photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ChatChannel chatChannel = channelList.get(getAdapterPosition());
                    if (! chatChannel.isGroup()) {
                        String id = chatChannel.getId();
                        String name = chatChannel.getName();
                        Intent profileIntent = new Intent(view.getContext(), UserProfileActivity.class);
                        profileIntent.putExtra(UserProfileActivity.EXTRA_STRING_USER_ID, id);
                        profileIntent.putExtra(UserProfileActivity.EXTRA_STRING_USER_NAME, name);
                        view.getContext().startActivity(profileIntent);
                    }
                }
            });
            channelName = (TextView) view.findViewById(R.id.tv_user_name);
            lastMessage = (TextView) view.findViewById(R.id.tv_last_chat);
            messageTime = (TextView) view.findViewById(R.id.tv_message_time);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            ChatChannel chatChannel = channelList.get(getAdapterPosition());
            chatChannel.setNewMessage(false);
            Intent intent = new Intent(v.getContext(), ConversationActivity.class);
            intent.putExtra(CHANNEL_ID, chatChannel.getId());
            v.getContext().startActivity(intent);
        }
    }
}
