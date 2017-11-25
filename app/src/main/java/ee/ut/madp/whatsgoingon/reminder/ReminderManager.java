package ee.ut.madp.whatsgoingon.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_NOTIFICATION_ID;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT;

/**
 * Created by admin on 24.11.2017.
 */

public class ReminderManager {
    private Context context;
    private AlarmManager alarmManager;

    public ReminderManager(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Set reminder for events at specified time
     * @param event
     * @param when
     */
    public void setReminder(Event event, long when) {

        Intent notificationIntent = new Intent(context, ReminderReceiver.class);
        notificationIntent.putExtra(EXTRA_EVENT, event);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, event.getId().hashCode(), notificationIntent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, when, alarmIntent);

    }

    /**
     * Repeats event reminder after 5 min
     * @param notificationId
     */
    public void setRepeatingAlarm(int notificationId) {
        Intent notificationIntent = new Intent(context, ReminderReceiver.class);
        notificationIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);


        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, 0);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 5 * 60 * 1000,
                5 * 60 * 1000, alarmIntent);
    }
}
