package ee.ut.madp.whatsgoingon.adapters;

/**
 * Created by admin on 28.10.2017.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.models.Event;

/**
 * Created by admin on 27.10.2017.
 */

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {
    private List<Event> eventList;
    private Context context;

    public EventAdapter(Context context, List<Event> eventList) {
        this.eventList = eventList;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_event_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.eventName.setText(event.getName());
        //holder.eventType.setText(event.get());
        holder.eventTime.setText(String.valueOf(event.getDateTime()));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView eventName;
        TextView eventType;
        TextView eventTime;
        // View onlineIndicator;

        public MyViewHolder(View view) {
            super(view);
            eventName = (TextView) view.findViewById(R.id.event_name);
            eventType = (TextView) view.findViewById(R.id.event_type);
            eventTime = (TextView) view.findViewById(R.id.event_time);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Event event = eventList.get(getAdapterPosition());
           // e.setNewMessage(false);
//            Intent intent = new Intent(v.getContext(), ConversationActivity.class);
//            intent.putExtra(PARCEL_CHAT_CHANNEL, chatChannel);
//            context.startActivity(intent);
        }
    }
}
