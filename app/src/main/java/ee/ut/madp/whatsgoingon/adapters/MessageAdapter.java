package ee.ut.madp.whatsgoingon.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT_ID;

/**
 * Adapter of chat messages to be displayed in conversations.
 *
 * Created by admin on 27.10.2017.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final String TAG = MessageAdapter.class.getSimpleName();

    private List<ChatMessage> chatMessages;
    private Context context;
    private Map<String, String> photosMap;

    public MessageAdapter(Context context, List<ChatMessage> chatMessages, Map<String, String> photosMap) {
        Log.i(TAG, "constructor");
        this.chatMessages = chatMessages;
        this.context = context;
        this.photosMap = photosMap;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder");
        MessageViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = null;
        // isMe
        if (viewType == 1) {
            view = inflater.inflate(R.layout.message_me, parent, false);
        } else if (viewType == 0) {
            view = inflater.inflate(R.layout.message_you, parent, false);
        }
        viewHolder = new MessageViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder");
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
        } else if (ChatHelper.isEventText(messageText)) {
            holder.messageEvent.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageEvent.getLayoutParams();

            params.addRule(RelativeLayout.BELOW, R.id.ll_auth_time);
            if (isMe(position)) {
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            }
            holder.messageEvent.setLayoutParams(params);

            holder.message.setVisibility(View.GONE);
            final String[] decodedEventMessage = ChatHelper.decodeEventMessage(ChatHelper.getEventText(messageText));
            holder.eventName.setText(decodedEventMessage[1]);
            holder.eventInfo.setText(decodedEventMessage[2]);
            holder.messageEvent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, EventFormActivity.class);
                    intent.putExtra(EXTRA_EVENT_ID, decodedEventMessage[0]);
                    context.startActivity(intent);
                }
            });
        } else {
            holder.messageEvent.setVisibility(View.GONE);
            holder.messagePicture.setVisibility(View.GONE);
            holder.message.setVisibility(View.VISIBLE);
            holder.message.setText(chatMessage.getMessageText());
        }
    }

    @Override
    public int getItemViewType(int position) {
        Log.i(TAG, "getItemViewType");
        return chatMessages.get(position).isMe() ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        Log.i(TAG, "getItemCount");
        return chatMessages.size();
    }

    private boolean isMe(int position) {
        Log.i(TAG, "isMe");
        return chatMessages.get(position).isMe();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder  {
        TextView messageTime, message, author;
        CircleImageView photo;
        ImageView messagePicture;
        RelativeLayout messageEvent;
        TextView eventName, eventInfo;

        public MessageViewHolder(View view) {
            super(view);
            Log.i(TAG, "MessageViewHolder.constructor");
            message = (TextView) view.findViewById(R.id.tv_message_text);
            messageTime = (TextView) view.findViewById(R.id.tv_message_time);
            author = (TextView) view.findViewById(R.id.tv_message_author);
            photo = (CircleImageView) view.findViewById(R.id.civ_message_user_photo);
            messagePicture = (ImageView) view.findViewById(R.id.iv_message_photo);

            messageEvent = (RelativeLayout) view.findViewById(R.id.message_event);
            eventName = (TextView) view.findViewById(R.id.event_name);
            eventInfo = (TextView) view.findViewById(R.id.event_info);

        }
    }
}
