package ee.ut.madp.whatsgoingon;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ee.ut.madp.whatsgoingon.models.ChatMessage;

/**
 * Created by dominikf on 3. 10. 2017.
 */

public class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {
    Context context;
    int layoutResourceId;
    ArrayList<ChatMessage> data = null;

    public ChatMessageAdapter(Context context, int resource, List<ChatMessage> objects) {
        super(context, resource, objects);
        this.layoutResourceId = resource;
        this.context = context;
        this.data = (ArrayList) objects;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        ChatMessageHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ChatMessageHolder();
            holder.author = (TextView)  row.findViewById(R.id.authorTextView);
            holder.message = (TextView) row.findViewById(R.id.messageTextView);
            holder.time = (TextView) row.findViewById(R.id.timeTextView);

            row.setTag(holder);
        } else {
            holder = (ChatMessageHolder) row.getTag();
        }

        ChatMessage item = data.get(position);
        holder.author.setText(item.getMessageAuthor());
        holder.message.setText(item.getMessageText());
        holder.time.setText(DateFormat.format("HH:mm:ss", item.getMessageTime()));

        return row;
    }

    private class ChatMessageHolder {
        TextView author;
        TextView message;
        TextView time;
    }
}
