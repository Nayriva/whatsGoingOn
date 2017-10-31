package ee.ut.madp.whatsgoingon.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
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
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.TimeComparator;
import ee.ut.madp.whatsgoingon.adapters.EventAdapter;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS_DATE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENT_DAY_REQUEST_CODE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PARAM_EVENT_DAY;

public class EventsOnDayActivity extends AppCompatActivity implements Observer {

    @BindView(R.id.recyclerView) RecyclerView recyclerView;
    @BindView(R.id.event_day)
    TextView eventDay;

    private ChatApplication application;
    private Intent data;
    private List<Event> eventList = new ArrayList<>();
    private EventAdapter eventAdapter;
    private DatabaseReference eventsRef;
    private ValueEventListener valueEventListener;
    private long eventDate;
    private String joinedEvent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_on_day);
        ButterKnife.bind(this);
        DialogHelper.showProgressDialog(EventsOnDayActivity.this, getString(R.string.progress_dialog_wait));
        setupRecyclerView();

        if (getIntent().hasExtra(PARAM_EVENT_DAY)) {
            eventDate = getIntent().getLongExtra(PARAM_EVENT_DAY, 0);
            eventDay.setText(DateHelper.parseDateFromLong(eventDate));
            setTitle("Events on " + DateHelper.parseDateFromLong(eventDate) );
        }


        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);

        eventsRef.orderByChild(FIREBASE_CHILD_EVENTS_DATE).equalTo(eventDate).addListenerForSingleValueEvent(setValueEventListener());


        application = (ChatApplication) getApplication();
        application.addObserver(this);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == EVENT_DAY_REQUEST_CODE) {
                if (getIntent().hasExtra(GeneralConstants.EXTRA_EVENT_JOINING)) {
                    joinedEvent = getIntent().getStringExtra(GeneralConstants.EXTRA_EVENT_JOINING);
                    if (eventList != null && joinedEvent != null) {
                        for (Event event : eventList) {
                            if (event.getId().equals(joinedEvent)) {
                                event.setJoining(true);
                            }
                            eventAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.deleteObserver(this);
        eventsRef.removeEventListener(valueEventListener);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            data = new Intent();
            setResult(Activity.RESULT_OK, data);
            finish();
        }
        return true;
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            case ChatApplication.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ChatApplication.GROUP_MESSAGE_RECEIVED: {
                //TODO show notification
            } break;
        }
    }

    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(EventsOnDayActivity.this, eventList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(this, R.drawable.horizontal_divider);
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);
        recyclerView.setAdapter(eventAdapter);
    }

    private ValueEventListener setValueEventListener() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Event event = postSnapshot.getValue(Event.class);
                    if ( event.getAttendantIds() != null && event.getAttendantIds().containsKey(UserHelper.getCurrentUserId())) {
                        event.setJoining(true);
                    } else {
                        event.setJoining(false);
                    }
                    if (event != null) {
                        eventList.add(event);
                    }
                }
                eventAdapter.notifyDataSetChanged();
                Collections.sort(eventList, new TimeComparator());
                DialogHelper.hideProgressDialog();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        return valueEventListener;
    }

}
