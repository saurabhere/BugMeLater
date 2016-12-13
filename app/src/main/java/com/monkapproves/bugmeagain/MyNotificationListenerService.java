package com.monkapproves.bugmeagain;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class MyNotificationListenerService extends NotificationListenerService {
    private String TAG = this.getClass().getSimpleName();
    private Set<StatusBarNotification> snoozedNotifs = new HashSet<>();

    private NLServiceReceiver nlservicereciver;
    @Override
    public void onCreate() {
        super.onCreate();
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.monkapproves.bugmelater.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        registerReceiver(nlservicereciver,filter);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn.getNotification().extras.getChar("replayed") == 'Y') {
            Log.i(TAG, "can't touch this.");
            return;
        }
        if(getCurrentInterruptionFilter() > NotificationManager.INTERRUPTION_FILTER_ALL) {
            snoozedNotifs.add(sbn);
            cancelNotification(sbn.getKey());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "********** onNOtificationRemoved");
    }
    public void flushAllSnoozedNotifs()
    {
        int i=1;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        Iterator<StatusBarNotification> notifs = snoozedNotifs.iterator();
        while(notifs.hasNext())
        {
            Notification notif = notifs.next().getNotification();
            notif.extras.putChar("replayed", 'Y');
            mNotifyMgr.notify(i++, notif);
        }
        snoozedNotifs.clear();
    }
    public void clearAllNotifs()
    {
        snoozedNotifs.clear();
        cancelAllNotifications();
    }
    class NLServiceReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("flush")){
                MyNotificationListenerService.this.flushAllSnoozedNotifs();
            } else if(intent.getStringExtra("command").equals("flush")){
                MyNotificationListenerService.this.clearAllNotifs();
            }
        }
    }
}
