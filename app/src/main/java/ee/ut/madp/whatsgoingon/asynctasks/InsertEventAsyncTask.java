package ee.ut.madp.whatsgoingon.asynctasks;

import android.util.Log;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.HashMap;

import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS;
import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS_GOOGLE_ID;

/**
 * Async task for inserting new event.
 * Created by admin on 25.11.2017.
 */

public class InsertEventAsyncTask extends CalendarAsyncTask {

    private static final String TAG = InsertEventAsyncTask.class.getSimpleName();

    private String firebaseEventId;
    private Event event;

    public InsertEventAsyncTask(EventFormActivity activity, Calendar service, String firebaseEventId, Event event) {
        super(activity, service);
        Log.i(TAG, "constructor");
        this.firebaseEventId = firebaseEventId;
        this.event = event;
    }

    @Override
    protected void doInBackground() throws IOException {
        Log.i(TAG, "doInBackground");
        String eventId =  client.events().insert("primary", event).execute().getId();
        if (eventId != null && event != null) {
            HashMap<String, Object> result = new HashMap<>();
            result.put(FIREBASE_CHILD_EVENTS_GOOGLE_ID, eventId);
            FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_EVENTS).child(firebaseEventId).updateChildren(result);
            EventFormActivity.getEvent().setGoogleEventId(eventId);
        }
    }
}
