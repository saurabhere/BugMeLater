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
import java.util.Timer;
import java.util.TimerTask;

public class MyNotificationListenerService extends NotificationListenerService {

    private TimerTask timerTask;
    private Timer timer;
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
        timerTask = new TimerTask() {
            @Override
            public void run() {
                onInterruptionFilterChanged(0);
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }
    private boolean isCriteriaMet()
    {
        Boolean block_notifs = preferences.getBoolean(SettingsActivity.PREF_KEY_BLOCK_NOTIFICATIONS, false);
        if(!block_notifs)
            return false;
        Boolean interruptions = preferences.getBoolean(SettingsActivity.PREF_KEY_INTERRUPTIONS, false);
        Boolean workHoursSwitch = preferences.getBoolean(SettingsActivity.PREF_KEY_WORK_HOURS, false);
        Long currentDateTime = new Date().getTime();
        Date d = new Date();
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        Long currentTime = currentDateTime - d.getTime() + d.getTimezoneOffset()*60*1000;
        if((interruptions && getCurrentInterruptionFilter() >=
                Integer.parseInt(preferences.getString(SettingsActivity.PREF_KEY_INTERRUPTION_FILTER, "0")))
                || (workHoursSwitch
                && preferences.getLong(SettingsActivity.PREF_KEY_WORK_HOURS_START, 0) <  currentTime
                && currentTime < preferences.getLong(SettingsActivity.PREF_KEY_WORK_HOURS_END, 0)
        ))
            return true;
        return false;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        if(notif.extras.getChar("replayed") == 'Y') {
            return;
        }
        if(isCriteriaMet())
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
        if(!isCriteriaMet())
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

    public void onPreferenceChanged(String key) {
        onInterruptionFilterChanged(0);
        switch (key) {
            case SettingsActivity.PREF_KEY_BLOCK_NOTIFICATIONS:
            case SettingsActivity.PREF_KEY_WORK_HOURS:
            case SettingsActivity.PREF_KEY_WORK_HOURS_END:
                if(timer != null)
                {
                    timer.cancel();
                    timerTask.cancel();
                    timer = null;
                }
                if (preferences.getBoolean(SettingsActivity.PREF_KEY_BLOCK_NOTIFICATIONS, false)
                        && preferences.getBoolean(SettingsActivity.PREF_KEY_WORK_HOURS, false))
                {
                    timer = new Timer();
                    Date date = new Date();
                    long endTime = preferences.getLong(SettingsActivity.PREF_KEY_WORK_HOURS_END, 0);
                    date.setHours((int) (endTime / 3600000));
                    date.setMinutes((int) (endTime % 3600000 / 60000) - date.getTimezoneOffset());
                    date.setSeconds(0);
                    timer.schedule(timerTask, date , 24*3600*1000);
                }
                break;
        }
    }

    class NLServiceReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("flush")){
                MyNotificationListenerService.this.flushAllSnoozedNotifs();
            } else if(intent.getStringExtra("command").equals("clearAll")){
                MyNotificationListenerService.this.clearAllNotifs();
            } else if(intent.getStringExtra("command").equals("prefChanged")) {
                MyNotificationListenerService.this.onPreferenceChanged(intent.getStringExtra("pref"));
            }
        }
    }
}
