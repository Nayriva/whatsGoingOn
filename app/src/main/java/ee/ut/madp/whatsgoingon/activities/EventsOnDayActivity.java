package ee.ut.madp.whatsgoingon.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.EventAdapter;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.comparators.EventComparator;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS_DATE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENT_DAY_REQUEST_CODE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EDITED_EVENT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT_DAY;

public class EventsOnDayActivity extends AppCompatActivity implements Observer {

    private static final String TAG = EventsOnDayActivity.class.getSimpleName();

    @BindView(R.id.recyclerView) RecyclerView recyclerView;
    @BindView(R.id.event_day) TextView eventDay;

    private ApplicationClass application;
    private List<Event> eventList;
    private EventAdapter eventAdapter;
    private DatabaseReference eventsRef;
    private ValueEventListener valueEventListener;
    private long dateOfEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_on_day);
        ButterKnife.bind(this);

        eventList = new ArrayList<>();
        setupRecyclerView();

        if (getIntent().hasExtra(EXTRA_EVENT_DAY)) {
            dateOfEvents = getIntent().getLongExtra(EXTRA_EVENT_DAY, 0);
            if (dateOfEvents == 0) {
                return;
            }
            eventDay.setText(DateHelper.parseDateFromLong(dateOfEvents));
            setTitle("Events on " + DateHelper.parseDateFromLong(dateOfEvents) );
        }


        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);

        eventsRef.orderByChild(FIREBASE_CHILD_EVENTS_DATE).equalTo(dateOfEvents)
                .addListenerForSingleValueEvent(setValueEventListener(this));

        application = (ApplicationClass) getApplication();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EVENT_DAY_REQUEST_CODE && resultCode == RESULT_OK) {
            //event has been joined
            if (data.hasExtra(GeneralConstants.EXTRA_JOINED_EVENT)) {
                Event joinedEvent = data.getParcelableExtra(GeneralConstants.EXTRA_JOINED_EVENT);
                List<String> attendants = data.getStringArrayListExtra(GeneralConstants.EXTRA_EVENT_ATTENDANTS);
                joinedEvent.setAttendantIds(attendants);
                if (eventList != null) {
                    for (Event event : eventList) {
                        if (event.getId().equals(joinedEvent.getId())) {
                            eventList.remove(event);
                            eventList.add(joinedEvent);
                            break;
                        }
                    }
                }
            }
            //event has been deleted
            if (data.hasExtra(GeneralConstants.EXTRA_DELETED_EVENT)) {
                String deletedEvent = data.getStringExtra(GeneralConstants.EXTRA_DELETED_EVENT);
                for (Event event: eventList) {
                    if (event.getId().equals(deletedEvent)) {
                        eventList.remove(event);
                        if (eventList.isEmpty()) {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(GeneralConstants.EXTRA_EVENT_DAY_BECAME_EMPTY, event.getDateTime());
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                        break;
                    }
                }
            }
            if (data.hasExtra(GeneralConstants.EXTRA_EDITED_EVENT)) {
                Event editedEvent = data.getParcelableExtra(EXTRA_EDITED_EVENT);
                List<String> attendants = data.getStringArrayListExtra(GeneralConstants.EXTRA_EVENT_ATTENDANTS);
                editedEvent.setAttendantIds(attendants);
                for (Event event: eventList) {
                    if (event.getId().equals(editedEvent.getId())) {
                        eventList.remove(event);
                    }
                }
                if (editedEvent.getDate() == dateOfEvents) {
                    eventList.add(editedEvent);
                }
            }
            Collections.sort(eventAdapter.getData(), new EventComparator());
            Collections.reverse(eventAdapter.getData());
            eventAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        application.addObserver(this);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        application.deleteObserver(this);
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        eventsRef.removeEventListener(valueEventListener);
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            case ApplicationClass.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ApplicationClass.GROUP_MESSAGE_RECEIVED: {
                ChatChannel chatChannel = application.getChannel(data);
                ChatMessage lastMessage = application.getLastMessage(data);
                if (chatChannel != null && lastMessage != null) {
                    MessageNotificationHelper.showNotification(this, chatChannel.getName(),
                            chatChannel.getLastMessage(), chatChannel.getId());
                }
            } break;
        }
    }

    private void setupRecyclerView() {
        Log.i(TAG, "setupRecyclerView");
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        eventAdapter = new EventAdapter(EventsOnDayActivity.this, eventList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(this, R.drawable.horizontal_divider);
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);
        recyclerView.setAdapter(eventAdapter);
    }

    private ValueEventListener setValueEventListener(final Context context) {
        Log.i(TAG, "setValueEventListener");
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "setValueEventListener.onDataChange");
                DialogHelper.showProgressDialog(context, getResources().getString(R.string.downloading_data));
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Event event = postSnapshot.getValue(Event.class);
                    if (event != null) {
                        if (event.getAttendantIds() != null
                                && event.getAttendantIds().contains(UserHelper.getCurrentUserId())) {
                            event.setJoined(true);
                        } else {
                            event.setJoined(false);
                        }
                        eventList.add(event);
                    }
                }
                eventAdapter.notifyDataSetChanged();
                DialogHelper.hideProgressDialog();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //left blank intentionally
            }
        };

        return valueEventListener;
    }
}
