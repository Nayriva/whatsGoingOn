package ee.ut.madp.whatsgoingon.asynctasks;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;

import java.io.IOException;

import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

/**
 * Created by admin on 25.11.2017.
 */

public class InsertEventAsyncTask extends CalendarAsyncTask {
    private Event event;

    public InsertEventAsyncTask(EventFormActivity activity, Calendar service, Event event) {
        super(activity, service);
        this.event = event;
    }

    @Override
    protected void doInBackground() throws IOException {
        String calendarId = "primary";
        client.events().insert(calendarId, event).execute();
    }
}
