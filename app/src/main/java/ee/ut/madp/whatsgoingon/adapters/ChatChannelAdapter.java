package ee.ut.madp.whatsgoingon.adapters;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.ConversationActivity;
import ee.ut.madp.whatsgoingon.activities.UserProfileActivity;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_CHANNEL_ID;

/**
 * Adapter of chat channels for recycler view. Implements as well onClick handling.
 *
 * Created by admin on 27.10.2017.
 */

public class ChatChannelAdapter extends RecyclerView.Adapter<ChatChannelAdapter.ChatChannelViewHolder> {

    private static final String TAG = ChatChannelAdapter.class.getSimpleName();

    private List<ChatChannel> channelList;

    public ChatChannelAdapter(List<ChatChannel> channelList) {
        Log.i(TAG, "constructor: " + channelList);
        this.channelList = channelList;
    }

    @Override
    public ChatChannelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_chat_channels, parent, false);

        return new ChatChannelViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ChatChannelViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder");
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
        Log.i(TAG, "getItemCount");
        return channelList.size();
    }

    public void addChannel(ChatChannel foundChannel) {
        Log.i(TAG, "addChannel: " + foundChannel);
        channelList.add(foundChannel);
    }

    public void removeChannel(ChatChannel channel) {
        Log.i(TAG, "removeChannel: " + channel);
        channelList.remove(channel);
    }

    public ChatChannel getChannelById(String id) {
        Log.i(TAG, "getChannelById: " + id);
        for (ChatChannel chatChannel: channelList) {
            if (chatChannel.getId().equals(id)) {
                return chatChannel;
            }
        }
        return null;
    }

    public List<ChatChannel> getChannels() {
        Log.i(TAG, "getChannels");
        return channelList;
    }

    public void clearChannels() {
        Log.i(TAG, "clearChannels");
        channelList.clear();
    }

    public class ChatChannelViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CircleImageView photo;
        TextView channelName;
        TextView lastMessage;
        TextView messageTime;

        public ChatChannelViewHolder(View view) {
            super(view);
            Log.i(TAG, "ChatChannelViewHolder.constructor");
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
            Log.i(TAG, "ChatChannelViewHolder.onClick");
            ChatChannel chatChannel = channelList.get(getAdapterPosition());
            chatChannel.setNewMessage(false);
            Intent intent = new Intent(v.getContext(), ConversationActivity.class);
            intent.putExtra(EXTRA_CHANNEL_ID, chatChannel.getId());
            v.getContext().startActivity(intent);
        }
    }
}
