package ee.ut.madp.whatsgoingon.helpers;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.R;

/**
 * Created by dominikf on 2. 11. 2017.
 */

public class MessageNotificationHelper {

    private static NotificationManager manager;

    public static void setManager(NotificationManager manager) {
        MessageNotificationHelper.manager = manager;
    }

    public static void showNotification(Context context, String sender, String text, String id) {
        if (ApplicationClass.notificationsOn || ApplicationClass.vibrateOn) {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(sender);
            if (ChatHelper.isImageText(text)) {
                builder.setContentText(context.getString(R.string.sent_picture));
            } else if (ChatHelper.isEventText(text)) {
                builder.setContentText(context.getString(R.string.shared_event));
            } else {
                builder.setContentText(text);
            }
            if (ApplicationClass.notificationsOn) {
                builder.setSound(ApplicationClass.getRingtone());
            }
            if (ApplicationClass.vibrateOn) {
                builder.setVibrate(new long[]{100, 200, 100});
            }
            manager.notify(id.hashCode(), builder.build());
        }
    }
}
