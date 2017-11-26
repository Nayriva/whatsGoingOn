package ee.ut.madp.whatsgoingon.reminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ee.ut.madp.whatsgoingon.helpers.NotificationHelper;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = ReminderReceiver.class.getSimpleName();

    public ReminderReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        if (intent.hasExtra(EXTRA_EVENT)) {
            Event event = intent.getParcelableExtra(EXTRA_EVENT);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(event.getId().hashCode(), NotificationHelper.createReminderNotification(context, event));
        }
    }
}
