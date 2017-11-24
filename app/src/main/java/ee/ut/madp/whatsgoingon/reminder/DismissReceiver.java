package ee.ut.madp.whatsgoingon.reminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.NOTIFICATION_ID;

public class DismissReceiver extends BroadcastReceiver {
    public static final String TAG = DismissReceiver.class.getSimpleName();
    public DismissReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(NOTIFICATION_ID)) {
            Log.i(TAG, "Dismiss upcoming event");

            int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
            Intent notificationIntent = new Intent(context, ReminderReceiver.class);
            // cancel notifying
            PendingIntent cancelIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(cancelIntent);

            // cancel notification from status bar
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
        }

    }
}
