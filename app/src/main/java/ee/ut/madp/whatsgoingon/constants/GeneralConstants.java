package ee.ut.madp.whatsgoingon.constants;

/**
 * General constants used through application
 */
public class GeneralConstants {
    // settings constants
    public static final int SPLASH_DISPLAY_LENGTH = 1000;
    public static final String CUSTOM_FONT = "Oh_Maria.ttf";

    // requests code constants
    public static final int SIGN_UP_REQUEST_CODE = 0;
    public static final int GOOGLE_SIGN_IN_REQUEST_CODE = 1;
    public static final int EVENTS_REQUEST_CODE = 2 ;
    public static final int EVENT_DAY_REQUEST_CODE = 3 ;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 4;
    public static final int REQUEST_AUTHORIZATION = 5;
    public static final int REQUEST_ACCOUNT_PICKER = 6;

    // intents extra constants
    public static final String EXTRA_CHANNEL_ID = "channelId";
    public static final String EXTRA_NOTIFICATION_ID = "notificationId";
    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_EVENT_DAY = "eventDay";
    public static final String EXTRA_JOINED_EVENT = "joinedEvent";
    public static final String EXTRA_DELETED_EVENT = "deletedEvent";
    public static final String EXTRA_EDITED_EVENT = "editedEvent";
    public static final String EXTRA_ADDED_EVENT = "createdEvent";
    public static final String EXTRA_EVENT_DAY_BECAME_EMPTY = "eventDayBecameEmpty" ;
    public static final String EXTRA_EVENT_ATTENDANTS = "eventAttendants";

    // date format constants
    public static final String FULL_DATE_TIME_FORMAT = "MMM dd, yyyy HH:mm";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATE_TIME_FORMAT = "MMM dd HH:mm";
    public static final String DATE_FORMAT = "MMM dd";
    public static final String FULL_DATE_FORMAT = "MMM dd yyyy";

    // shared preferences constants
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String PREF_PASSWORD = "password";
    public static final String PREF_MESSAGE_NOTIFICATION = "notifications_message";
    public static final String PREF_NOTIFICATION_VIBRATE = "notification_vibrate";
    public static final String PREF_NOTIFICATION_RINGTONE = "notification_ringtone";
    public static final String PREF_REMINDER_HOURS_BEFORE = "reminder_hours_before";
    public static final String PREF_REMINDERS = "notification_reminders";

}