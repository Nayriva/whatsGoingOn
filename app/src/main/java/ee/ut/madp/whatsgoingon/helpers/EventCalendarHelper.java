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

import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;

import ee.ut.madp.whatsgoingon.activities.EventFormActivity;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS;
import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_EVENTS_ID;

/**
 * Created by admin on 23.11.2017.
 */

public class EventCalendarHelper {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 5;

    public static void insertEvent(Context context, Event event, String calendarType) {

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

    public static void updateEvent(Context context, Event event) {
        ContentResolver cr = context.getContentResolver();
        Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.getEventId());
        cr.update(eventUri, createEventValues(context, event), null, null);

    }

    public static void deleteEvent(Context context, long id) {
        Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        context.getContentResolver().delete(eventUri, null, null);

    }

    private static ContentValues createEventValues(Context context, Event event) {
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
}
