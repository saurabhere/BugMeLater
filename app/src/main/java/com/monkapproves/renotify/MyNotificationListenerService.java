package com.monkapproves.renotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MyNotificationListenerService extends NotificationListenerService {
    private String TAG = this.getClass().getSimpleName();
    private Map<String, StatusBarNotification> snoozedNotifs = new HashMap<String, StatusBarNotification>();
    SharedPreferences preferences;
    private NLServiceReceiver nlservicereciver;
    @Override
    public void onCreate() {
        super.onCreate();
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.monkapproves.bugmelater.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        registerReceiver(nlservicereciver, filter);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        if(notif.extras.getChar("replayed") == 'Y') {
            return;
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean block_notifs = preferences.getBoolean("pref_key_block_notifications", false);
        if(!block_notifs)
            return;
        Boolean interruptions = preferences.getBoolean("pref_key_block_by_interruption", false);
        Boolean workHoursSwitch = preferences.getBoolean("pref_key_work_hours", false);
        Long currentDateTime = new Date().getTime();
        Date d = new Date();
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        Long currentTime = currentDateTime - d.getTime() + d.getTimezoneOffset()*60*1000;
        if((interruptions && getCurrentInterruptionFilter() >=
                Integer.parseInt(preferences.getString("pref_key_interruption_filter_settings", "0")))
                || (workHoursSwitch
                    && preferences.getLong("pref_key_work_hours_start", 0) <  currentTime
                    && currentTime < preferences.getLong("pref_key_work_hours_end", 0)
                    ))
        {
            if (sbn.isOngoing()
                    || !sbn.isClearable()
                    || (notif.category != null &&
                        (notif.category.equals(Notification.CATEGORY_PROGRESS)
                        || notif.category.equals(Notification.CATEGORY_CALL)
                        || notif.category.equals(Notification.CATEGORY_ERROR)
                        || notif.category.equals(Notification.CATEGORY_SYSTEM)
                        || notif.category.equals(Notification.CATEGORY_SERVICE)
                        || notif.category.equals(Notification.CATEGORY_TRANSPORT)))
                    ) {
                Log.i(TAG, "Not Targeting these notifs. Let them be.");
                return;
            }
            snoozedNotifs.put(sbn.getKey(), sbn);
            cancelNotification(sbn.getKey());
        }
    }
    @Override
    public void onInterruptionFilterChanged(int interruptionFilter)
    {
        if(interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL)
            flushAllSnoozedNotifs();
    }
    public void flushAllSnoozedNotifs()
    {
        int i=1;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        Iterator notifs = snoozedNotifs.entrySet().iterator();
        while(notifs.hasNext())
        {
            Map.Entry pair = (Map.Entry)notifs.next();
            StatusBarNotification sbn = ((StatusBarNotification)pair.getValue());
            Notification notif = sbn.getNotification();
            notif.extras.putChar("replayed", 'Y');
/*            Notification.Action actions[] = new Notification.Action[notif.actions.length+1];
            int k=0;
            for(Notification.Action action : notif.actions)
            {
                actions[k++] = action;
            }
            actions[k] = new Notification.Action.Builder(R.drawable.ic_info_black_24dp,
                    getString(R.string.pref_snooze_title), null).build();
            notif.actions = actions;*/
            mNotifyMgr.notify(sbn.getId(), notif);
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
            } else if(intent.getStringExtra("command").equals("clearAll")){
                MyNotificationListenerService.this.clearAllNotifs();
            }
        }
    }
}
