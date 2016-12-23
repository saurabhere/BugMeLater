package com.monkapproves.renotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MyNotificationListenerService extends NotificationListenerService {

    private TimerTask timerTask;
    private Timer timer;
    private String TAG = this.getClass().getSimpleName();
    private Map<String, StatusBarNotification> snoozedNotifs = new HashMap<String, StatusBarNotification>();
    SharedPreferences preferences;
    private NLServiceReceiver nlservicereciver;
    private NotificationCompat.Builder reminderNotificationBuilder;
    private boolean reminderNotificationBeingShown;
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

        Intent intent = new Intent("com.monkapproves.bugmelater.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        intent.putExtra("command", "flush");
        Intent resultIntent = new Intent(this, SettingsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(SettingsActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        reminderNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_action_renotify)
                .setContentTitle(getString(R.string.reminder_notification_title))
                .setContentText(getString(R.string.reminder_notification_text))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentIntent(resultPendingIntent)
                .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_info_black_24dp, getString(R.string.show_all), actionPendingIntent)
                        .build());
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
        {
            hideReminderNotification();
            return false;
        }
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
        {
            showReminderNotification();
            return true;
        }
        hideReminderNotification();
        return false;
    }
    private void showReminderNotification()
    {
        if(!reminderNotificationBeingShown && preferences.getBoolean(SettingsActivity.PREF_SHOW_REMINDER, false)) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(999, reminderNotificationBuilder.build());
            reminderNotificationBeingShown = true;
        }
    }
    private void updateReminderNotification()
    {
        if(reminderNotificationBeingShown && preferences.getBoolean(SettingsActivity.PREF_SHOW_REMINDER, false))
        {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if(snoozedNotifs.size() == 0)
                reminderNotificationBuilder.setContentText(getString(R.string.reminder_notification_text));
            else if(snoozedNotifs.size() == 1)
                reminderNotificationBuilder.setContentText(getString(R.string.reminder_notification_text_with_count, snoozedNotifs.size()));
            else
                reminderNotificationBuilder.setContentText(getString(R.string.reminder_notification_text_with_count_plural, snoozedNotifs.size()));
            notificationManager.notify(999, reminderNotificationBuilder.build());
        }
    }
    private void hideReminderNotification()
    {
        if(reminderNotificationBeingShown)
        {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(999);
            reminderNotificationBeingShown = false;
        }
    }
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        if(notif.extras.getChar("replayed") == 'Y') {
            return;
        }
        boolean isExcluded = preferences.getStringSet("excludeApps", new HashSet<String>())
                .contains(sbn.getPackageName());
        if(!isExcluded && isCriteriaMet())
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
            updateReminderNotification();
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
        updateReminderNotification();
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
            case SettingsActivity.PREF_SHOW_REMINDER:
                if(preferences.getBoolean(SettingsActivity.PREF_SHOW_REMINDER, false))
                {
                    if(isCriteriaMet())
                        showReminderNotification();
                }
                else
                {
                    hideReminderNotification();
                }
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
