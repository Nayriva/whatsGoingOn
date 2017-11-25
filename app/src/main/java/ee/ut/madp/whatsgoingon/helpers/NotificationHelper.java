package ee.ut.madp.whatsgoingon.helpers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;
import ee.ut.madp.whatsgoingon.activities.MainActivity;
import ee.ut.madp.whatsgoingon.models.Event;
import ee.ut.madp.whatsgoingon.reminder.DismissReceiver;
import ee.ut.madp.whatsgoingon.reminder.SnoozeReceiver;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_NOTIFICATION_ID;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT;

/**
 * Created by admin on 24.11.2017.
 */

public class NotificationHelper {

    public static final String TAG = NotificationHelper.class.getSimpleName();

    /**
     * Creates a notification for reminders
     * @param context
     * @param event
     * @return
     */
    public static Notification createReminderNotification(Context context, Event event) {
        Log.d(TAG, "createReminderNotification");

        int notificationId = event.getId().hashCode();

        NotificationCompat.Builder builder = getNotificationBuilder(context, event.getName(),
                DateHelper.parseDateFromLong(event.getDateTime()) + ", " + event.getPlace());
        Intent resultIntent = new Intent(context, EventFormActivity.class);
        resultIntent.putExtra(EXTRA_EVENT, event);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);

        // add actions such as snooze and dismiss
        builder.addAction(android.R.color.transparent, context.getResources().getString(R.string.dismiss), createDismissIntent(context, event.getId().hashCode()));
        builder.addAction(android.R.color.transparent, context.getResources().getString(R.string.snooze), createSnoozeIntent(context, event.getId().hashCode()));

        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    /**
     * Retuns instance of builder
     * @param context
     * @param title
     * @param message
     * @return
     */
    private static NotificationCompat.Builder getNotificationBuilder(Context context, String title, String message) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        // ask for sound
        String ringtonePreference = preference.getString("notification_ringtone", "DEFAULT_SOUND");

        NotificationCompat.Builder builder  =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_event)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setWhen(System.currentTimeMillis())
                        .setOnlyAlertOnce(true)
                        .setSound(Uri.parse(ringtonePreference))
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setAutoCancel(true);

        if (preference.getBoolean("notification_vibrate", true)) {
            // vibration
            builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        }

        return builder;
    }

    /**
     * Creates dismiss intent
     * @param context
     * @param notificationId
     * @return
     */
    private static PendingIntent createDismissIntent(Context context, int notificationId) {
        Intent dismissIntent = new Intent(context, DismissReceiver.class);
        dismissIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getBroadcast(context, notificationId, dismissIntent, 0);
    }

    /**
     * Creates snooze intent
     * @param context
     * @param notificationId
     * @return
     */
    private static PendingIntent createSnoozeIntent(Context context, int notificationId) {
        Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
        snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getBroadcast(context, notificationId, snoozeIntent, 0);
    }
}
