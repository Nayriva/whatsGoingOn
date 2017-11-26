package ee.ut.madp.whatsgoingon.reminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_NOTIFICATION_ID;

public class SnoozeReceiver extends BroadcastReceiver {
    public static final String TAG = SnoozeReceiver.class.getSimpleName();
    public SnoozeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.hasExtra(EXTRA_NOTIFICATION_ID)) {
            int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);

            Log.d(TAG, "Start snoozing");
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            new ReminderManager(context).setRepeatingAlarm(notificationId);


        }
    }
}
