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
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;
import ee.ut.madp.whatsgoingon.asynctasks.DeleteEventAsyncTask;
import ee.ut.madp.whatsgoingon.asynctasks.InsertEventAsyncTask;
import ee.ut.madp.whatsgoingon.asynctasks.UpdateEventAsyncTask;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS;
import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS_ID;
import static ee.ut.madp.whatsgoingon.constants.PermissionConstants.PERMISSIONS_GROUP_TWO;

/**
 * Created by admin on 23.11.2017.
 */

public class EventCalendarHelper {

    static String[] PERMISSIONS = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_CALENDAR};

    private static GoogleAccountCredential googleAccountCredential;

    public static void insertEvent(Context context, ee.ut.madp.whatsgoingon.models.Event event, ArrayList calendarTypes) {

        if (calendarTypes.contains(context.getResources().getStringArray(R.array.calendar_types)[0])) {
            addEventToLocalCalendar(context, event);
        }

        if (calendarTypes.contains(context.getResources().getStringArray(R.array.calendar_types)[1])) {
            addEventToGoogleCalendar(context, event);
        }
    }

    public static void updateEvent(Context context, ee.ut.madp.whatsgoingon.models.Event event) {
        if (event.getEventId() != 0) {
            // update event in local calendar
            ContentResolver cr = context.getContentResolver();
            Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.getEventId());
            cr.update(eventUri, createEventValues(event), null, null);
        }

        if (event.getGoogleEventId() != null)
            // update event in Google calendar
            new UpdateEventAsyncTask((EventFormActivity) context, EventCalendarHelper.initializeCalendarService(context),
                    EventFormActivity.getEvent().getGoogleEventId(), EventCalendarHelper.createGoogleEvent(event)).execute();

    }

    public static void deleteEvent(Context context, long eventId, String googleEventId) {
        if (eventId != 0) {
            // delete event from local calendar
            Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
            context.getContentResolver().delete(eventUri, null, null);
        }
        // delete vent from Google calendar
        if (googleEventId != null)
            new DeleteEventAsyncTask((EventFormActivity) context, EventCalendarHelper.initializeCalendarService(context),
                    googleEventId).execute();

    }

    private static ContentValues createEventValues(ee.ut.madp.whatsgoingon.models.Event event) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
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

    private static void addEventToLocalCalendar(Context context, ee.ut.madp.whatsgoingon.models.Event event) {
        ContentResolver cr = context.getContentResolver();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, PERMISSIONS_GROUP_TWO);
            return;
        }
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, createEventValues(event));
        long eventID = Long.parseLong(uri.getLastPathSegment());
        EventFormActivity.getEvent().setEventId(eventID);

        // add eventId
        HashMap<String, Object> result = new HashMap<>();
        result.put(FIREBASE_CHILD_EVENTS_ID, eventID);

        FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_EVENTS).child(event.getId()).updateChildren(result);
        Toast.makeText(context, context.getString(R.string.synchronized_event), Toast.LENGTH_SHORT).show();
    }


    private static void addEventToGoogleCalendar(Context context, ee.ut.madp.whatsgoingon.models.Event event) {
        googleAccountCredential = GoogleAccountHelper.getGoogleAccountCredential(context);
        if (GoogleAccountHelper.checkGooglePlayServicesAvailable((Activity) context)) {
            GoogleAccountHelper.haveGooglePlayServices((Activity) context, googleAccountCredential);
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, PERMISSIONS_GROUP_TWO);
            return;
        }

        Calendar service = initializeCalendarService(context);
        new InsertEventAsyncTask((EventFormActivity) context, service, event.getId(), createGoogleEvent(event)).execute();

    }

    public static Calendar initializeCalendarService(Context context) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, GoogleAccountHelper.getGoogleAccountCredential(context))
                .setApplicationName("What's Going On")
                .build();
    }

    public static Event createGoogleEvent(ee.ut.madp.whatsgoingon.models.Event event) {
        Event newEvent = new Event()
                .setSummary(event.getName())
                .setLocation(event.getPlace())
                .setDescription(event.getDescription());

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

        return newEvent;
    }

}
