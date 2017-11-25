package ee.ut.madp.whatsgoingon.asynctasks;

import com.google.api.services.calendar.Calendar;

import java.io.IOException;

import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

/**
 * Created by admin on 25.11.2017.
 */

public class DeleteEventAsyncTask extends CalendarAsyncTask {
    private String eventId;

    public DeleteEventAsyncTask(EventFormActivity activity, Calendar client, String googleEventId) {
        super(activity, client);
        this.eventId = googleEventId;
    }

    @Override
    protected void doInBackground() throws IOException {
        client.events().delete("primary", eventId).execute();
    }
}
