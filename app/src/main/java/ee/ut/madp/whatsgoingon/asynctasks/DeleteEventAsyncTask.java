package ee.ut.madp.whatsgoingon.asynctasks;

import android.util.Log;

import com.google.api.services.calendar.Calendar;

import java.io.IOException;

import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

/**
 * Async task for deleting event.
 * Created by admin on 25.11.2017.
 */

public class DeleteEventAsyncTask extends CalendarAsyncTask {

    private static final String TAG = DeleteEventAsyncTask.class.getSimpleName();

    private String eventId;

    public DeleteEventAsyncTask(EventFormActivity activity, Calendar client, String googleEventId) {
        super(activity, client);
        Log.i(TAG, "constructor");
        this.eventId = googleEventId;
    }

    @Override
    protected void doInBackground() throws IOException {
        Log.i(TAG, "doInBackground");
        client.events().delete("primary", eventId).execute();
    }
}
