package ee.ut.madp.whatsgoingon.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

/**
 * Created by admin on 27.10.2017.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    private List<ChatMessage> chatMessages;

    public MessageAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public MessageAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = null;
        // isMe
        if (viewType == 1) {
            view = inflater.inflate(R.layout.message_me, parent, false);
        } else if (viewType == 0) {
            view = inflater.inflate(R.layout.message_you, parent, false);
        }
        viewHolder = new MyViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MessageAdapter.MyViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        holder.message.setText(chatMessage.getMessageText());
        holder.messageTime.setText(chatMessage.getMessageText());
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).isMe() ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder  {
        TextView messageTime, message;

        public MyViewHolder(View view) {
            super(view);
            message = (TextView) view.findViewById(R.id.tv_chat_text);
            messageTime = (TextView) view.findViewById(R.id.tv_time);
        }

    }
}
