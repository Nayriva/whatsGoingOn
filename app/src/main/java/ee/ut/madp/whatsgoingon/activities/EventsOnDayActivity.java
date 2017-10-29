package ee.ut.madp.whatsgoingon.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.EventAdapter;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.models.Event;

public class EventsOnDayActivity extends AppCompatActivity implements Observer {

    @BindView(R.id.recyclerView) RecyclerView recyclerView;
    @BindView(R.id.event_day)
    TextView eventDay;

    private ChatApplication application;
    private Intent data;
    private List<Event> eventList = new ArrayList<>();
    private EventAdapter eventAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_on_day);
        ButterKnife.bind(this);

        eventList.add(new Event("eeie", "nova", "nova,", 5));
        eventList.add(new Event("eeie", "nova", "nova,", 5));
        eventList.add(new Event("eeie", "nova", "nova,", 5));
        eventList.add(new Event("eeie", "nova", "nova,", 5));

        eventDay.setText("neco");



        application = (ChatApplication) getApplication();
        application.addObserver(this);
        setupRecyclerView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.deleteObserver(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            data = new Intent();
            // if (saved )
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
        eventAdapter = new EventAdapter(this, eventList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(eventAdapter);
    }

    private void getAllEventsOnDay() {
        
    }
}
