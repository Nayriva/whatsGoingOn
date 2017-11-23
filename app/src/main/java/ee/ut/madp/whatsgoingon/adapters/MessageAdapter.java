package ee.ut.madp.whatsgoingon.adapters;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

/**
 * Created by admin on 27.10.2017.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    private List<ChatMessage> chatMessages;
    private Context context;
    private Map<String, String> photosMap;

    public MessageAdapter(Context context, List<ChatMessage> chatMessages, Map<String, String> photosMap) {
        this.chatMessages = chatMessages;
        this.context = context;
        this.photosMap = photosMap;
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
    public void onBindViewHolder(final MessageAdapter.MyViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        holder.author.setText(chatMessage.getDisplayName());
        holder.messageTime.setText(DateHelper.createMessageTime(context, chatMessage.getMessageTime()));
        holder.photo.setImageBitmap(ImageHelper.decodeBitmap(photosMap.get(chatMessage.getSender())));
        final String messageText = chatMessage.getMessageText();
        if (ChatHelper.isImageText(messageText)) {
            holder.messagePicture.setVisibility(View.VISIBLE);
            holder.messagePicture.setImageBitmap(
                    ImageHelper.decodeBitmap(ChatHelper.getImageBase64(messageText))
            );
            holder.message.setVisibility(View.GONE);
            holder.messagePicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                    dialog.setContentView(R.layout.dialog_image_fullscreen);
                    ImageView fullscreen = (ImageView) dialog.findViewById(R.id.iv_photo_fullscreen);
                    fullscreen.setImageBitmap(ImageHelper.decodeBitmap(ChatHelper.getImageBase64(messageText)));
                    dialog.show();
                }
            });
        } else {
            holder.message.setText(chatMessage.getMessageText());
        }
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
        TextView messageTime, message, author;
        CircleImageView photo;
        ImageView messagePicture;

        public MyViewHolder(View view) {
            super(view);
            message = (TextView) view.findViewById(R.id.tv_message_text);
            messageTime = (TextView) view.findViewById(R.id.tv_message_time);
            author = (TextView) view.findViewById(R.id.tv_message_author);
            photo = (CircleImageView) view.findViewById(R.id.civ_message_user_photo);
            messagePicture = (ImageView) view.findViewById(R.id.iv_message_photo);
        }
    }
}
