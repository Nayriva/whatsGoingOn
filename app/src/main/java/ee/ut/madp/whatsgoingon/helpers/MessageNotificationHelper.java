package ee.ut.madp.whatsgoingon.helpers;

import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;

import java.util.Random;

import ee.ut.madp.whatsgoingon.R;

/**
 * Created by dominikf on 2. 11. 2017.
 */

public class MessageNotificationHelper {

    private static NotificationManager manager;

    public static void setManager(NotificationManager manager) {
        MessageNotificationHelper.manager = manager;
    }

    public static void showNotification(Context context, String sender, String text) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle(sender)
                        .setContentText(text)
                        .setVibrate(new long[] { 100, 200, 100} )
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        manager.notify(new Random().nextInt(), builder.build());
    }
}
