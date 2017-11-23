package ee.ut.madp.whatsgoingon.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS;
import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS_ID;

//import ee.ut.madp.whatsgoingon.models.Event;

/**
 * Created by admin on 23.11.2017.
 */

public class EventCalendarHelper {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 5;
    private static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 6;

    public static void insertEvent(Context context, ee.ut.madp.whatsgoingon.models.Event event, ArrayList calendarTypes) {

        if (calendarTypes.contains(context.getResources().getStringArray(R.array.calendar_types)[0])) {
            addEventToLocalCalendar(context, event);
        }

        if (calendarTypes.contains(context.getResources().getStringArray(R.array.calendar_types)[1])) {
            addEventToGoogleCalendar(context, event);
        }


    }

    public static void updateEvent(Context context, ee.ut.madp.whatsgoingon.models.Event event) {
        ContentResolver cr = context.getContentResolver();
        Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.getEventId());
        cr.update(eventUri, createEventValues(context, event), null, null);

    }

    public static void deleteEvent(Context context, long id) {
        Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        context.getContentResolver().delete(eventUri, null, null);

    }

    private static ContentValues createEventValues(Context context, ee.ut.madp.whatsgoingon.models.Event event) {
        Calendar cal = Calendar.getInstance();
        java.util.TimeZone tz = cal.getTimeZone();
        ContentValues values = new ContentValues();

        values.put(CalendarContract.Events.CALENDAR_ID, 1);
        values.put(CalendarContract.Events.TITLE, event.getName());
        values.put(CalendarContract.Events.DESCRIPTION, event.getDescription());
        values.put(CalendarContract.Events.EVENT_LOCATION, event.getPlace());
        values.put(CalendarContract.Events.DTSTART, event.getDateTime());
        values.put(CalendarContract.Events.DTEND, event.getDateTime());
        values.put(CalendarContract.Events.EVENT_TIMEZONE, tz.getDisplayName());

        return values;
    }

    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    private static final String PREF_ACCOUNT_NAME = "accountName";

    private static void addEventToGoogleCalendar(Context context, ee.ut.madp.whatsgoingon.models.Event event) {
        Event newEvent = new Event()
                .setSummary("Google I/O 2015")
                .setLocation("800 Howard St., San Francisco, CA 94103")
                .setDescription("A chance to hear more about Google's developer products.");

        DateTime startDateTime = new DateTime(event.getDateTime());
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("America/Los_Angeles");
        newEvent.setStart(start);

        DateTime endDateTime = new DateTime(event.getDateTime());
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("America/Los_Angeles");
        newEvent.setEnd(end);


        String calendarId = "primary";

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
            return;
        }

        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
                context.getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
//        String accountName = (Activity)context.getPrefence(Context.MODE_PRIVATE)
//                .getString(PREF_ACCOUNT_NAME, null);
//        if (accountName != null) {
//            mCredential.setSelectedAccountName(accountName);
//        }

        initialize(context, mCredential);
        try {
            newEvent = mService.events().insert(calendarId, newEvent).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static com.google.api.services.calendar.Calendar mService = null;
    public static void initialize(Context context, GoogleAccountCredential credential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("What's Going On")
                .build();
    }

    private static void addEventToLocalCalendar(Context context, ee.ut.madp.whatsgoingon.models.Event event) {
        ContentResolver cr = context.getContentResolver();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_CALENDAR},
                    MY_PERMISSIONS_REQUEST_WRITE_CALENDAR);
            return;
        }
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, createEventValues(context, event));
        long eventID = Long.parseLong(uri.getLastPathSegment());
        EventFormActivity.getEvent().setEventId(eventID);

        // add eventId
        HashMap<String, Object> result = new HashMap<>();
        result.put(FIREBASE_CHILD_EVENTS_ID, eventID);

        FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_EVENTS).child(event.getId()).updateChildren(result);
    }
}
