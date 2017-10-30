package ee.ut.madp.whatsgoingon.adapters;

/**
 * Created by admin on 28.10.2017.
 */

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PARCEL_EVENT;

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
        if (UserHelper.getCurrentUserId().equals(event.getOwner())) {
            holder.eventType.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
        } else if (event.isJoining()) {
            holder.eventType.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
        }

        holder.eventTime.setText(String.valueOf(DateHelper.parseTimeFromLong(event.getDateTime())));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView eventName;
        TextView eventType;
        TextView eventTime;

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
            Intent intent = new Intent(v.getContext(), EventFormActivity.class);
            intent.putExtra(PARCEL_EVENT, event);
            context.startActivity(intent);
        }
    }
}
