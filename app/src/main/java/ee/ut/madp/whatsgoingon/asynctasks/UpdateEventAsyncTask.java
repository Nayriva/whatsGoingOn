package ee.ut.madp.whatsgoingon.asynctasks;

import android.util.Log;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;

import java.io.IOException;

import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

/**
 * Async task for updating events.
 *
 * Created by admin on 25.11.2017.
 */

public class UpdateEventAsyncTask extends CalendarAsyncTask {

    private final static String TAG = UpdateEventAsyncTask.class.getSimpleName();

    private Event event;
    private String eventId;

    public UpdateEventAsyncTask(EventFormActivity activity, Calendar client, String googleEventId, Event event) {
        super(activity, client);
        Log.i(TAG, "constructor");
        this.event = event;
        this.eventId = googleEventId;
    }

    @Override
    protected void doInBackground() throws IOException {
        Log.i(TAG, "doInBackground");
        client.events().update("primary", eventId, event).execute();
    }
}
