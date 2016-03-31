package net.brainas.android.app.UI;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import net.brainas.android.app.R;
import net.brainas.android.app.activities.MainActivity;


/**
 * Created by Kit Ushakov on 3/30/2016.
 */
public class NotificationController {
    private static int activeNotificationId = 001;
    private int activationCountTotal = 0;
    public  NotificationController() {}
    public void createActiveNotification(Context context, int activationCount) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setSmallIcon(R.drawable.ba_logo_64_mono);
        //notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ba_logo_64_mono));
        notificationBuilder.setContentTitle("Brainas remider");
        notificationBuilder.setContentText("You've received new active tasks");
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        activationCountTotal = activationCountTotal + activationCount;

        notificationBuilder.setNumber(activationCountTotal);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(alarmSound);

        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);

        mNotifyMgr.notify(activeNotificationId, notificationBuilder.build());
    }

    public static void removeActivationNotifications(Context context) {
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyMgr.cancel(activeNotificationId);
    }
}
