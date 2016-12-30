package com.monkapproves.renotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;
import com.bluelinelabs.logansquare.typeconverters.NotificationConverter;
import com.bluelinelabs.logansquare.typeconverters.PendingIntentConverter;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.monkapproves.renotify.settings.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MyNotificationListenerService extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();
    private TimerTask timerTask;
    private Timer timer;
    SharedPreferences mPreferences;
    private NLServiceReceiver nlServiceReceiver;
    private NotificationCompat.Builder reminderNotificationBuilder;
    private boolean reminderNotificationBeingShown;
    private RenotifyDbHelper mDbHelper;
    private long snoozedNotifs;

    private class MyTimerTask extends TimerTask {
        public void run() {
            onInterruptionFilterChanged(0);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDbHelper = new RenotifyDbHelper(getApplicationContext());

        nlServiceReceiver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        // @Todo: Fix incorrect package
        filter.addAction("com.monkapproves.bugmelater.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        registerReceiver(nlServiceReceiver, filter);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = new Intent("com.monkapproves.bugmelater.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        intent.putExtra("command", "flush");
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        reminderNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(getString(R.string.reminder_notification_title))
                .setContentText(getString(R.string.reminder_notification_text))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentIntent(resultPendingIntent)
                .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_info_black_24dp, getString(R.string.show_all), actionPendingIntent)
                        .build());

        snoozedNotifs = getNotifCount();
    }

    private long getNotifCount() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String selection = RenotifyContract.NotifEntry.COLUMN_NAME_REPOSTED + " = ?";
        String[] selectionArgs = { "0" };
        long cnt  = DatabaseUtils.queryNumEntries(db, RenotifyContract.NotifEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
        return cnt;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlServiceReceiver);
    }

    // @Todo: Check for system package
    private boolean isCriteriaMet() {
        Boolean interruptions = mPreferences.getBoolean(MainActivity.PREF_KEY_INTERRUPTIONS, false);
        Boolean workHoursSwitch = mPreferences.getBoolean(MainActivity.PREF_KEY_WORK_HOURS, false);
        Long currentDateTime = new Date().getTime();
        Date d = new Date();
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        Long currentTime = currentDateTime - d.getTime() + d.getTimezoneOffset()*60*1000;
        if((interruptions && getCurrentInterruptionFilter() >=
                Integer.parseInt(mPreferences.getString(MainActivity.PREF_KEY_INTERRUPTION_FILTER, "0")))
                || (workHoursSwitch
                && mPreferences.getLong(MainActivity.PREF_KEY_WORK_HOURS_START, 0) <  currentTime
                && currentTime < mPreferences.getLong(MainActivity.PREF_KEY_WORK_HOURS_END, 0)
        )) {
            showReminderNotification();
            return true;
        }
        hideReminderNotification();
        return false;
    }

    private void showReminderNotification() {
        if(!reminderNotificationBeingShown && mPreferences.getBoolean(MainActivity.PREF_SHOW_REMINDER, false)) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(999, reminderNotificationBuilder.build());
            reminderNotificationBeingShown = true;
        }
    }

    private void updateReminderNotification() {
        if(reminderNotificationBeingShown && mPreferences.getBoolean(MainActivity.PREF_SHOW_REMINDER, false)) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if(snoozedNotifs == 0)
                reminderNotificationBuilder.setContentText(getString(R.string.reminder_notification_text));
            else if(snoozedNotifs == 1)
                reminderNotificationBuilder.setContentText(getString(R.string.reminder_notification_text_with_count, snoozedNotifs));
            else
                reminderNotificationBuilder.setContentText(getString(R.string.reminder_notification_text_with_count_plural, snoozedNotifs));
            notificationManager.notify(999, reminderNotificationBuilder.build());
        }
    }

    private void hideReminderNotification() {
        if(reminderNotificationBeingShown) {
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
        boolean isExcluded = mPreferences.getStringSet("excludeApps", new HashSet<String>())
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

            saveNotification(sbn);
            updateReminderNotification();
        }
    }

    private void saveNotification(StatusBarNotification sbn) {
        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = null;
        ByteArrayOutputStream outputStream = null;
        try {
            outputStream = new ByteArrayOutputStream();
            generator = factory.createGenerator(outputStream, JsonEncoding.UTF8);

            Context context = createPackageContext(sbn.getPackageName(), 0);
            LoganSquare.registerTypeConverter(PendingIntent.class, new PendingIntentConverter(context));
            NotificationConverter converter = new NotificationConverter(context);
            converter.serialize(sbn.getNotification(), "sbn", false, generator);
            generator.close();
            outputStream.close();
            Log.v(TAG, "Serialized Notif | " + outputStream.toString());

            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(RenotifyContract.NotifEntry.COLUMN_NAME_PACKAGE, sbn.getPackageName());
            values.put(RenotifyContract.NotifEntry.COLUMN_NAME_SBN_KEY, sbn.getKey());
            values.put(RenotifyContract.NotifEntry.COLUMN_NAME_NOTIFICATION, outputStream.toString());
            db.insert(RenotifyContract.NotifEntry.TABLE_NAME, null, values);
            db.close();
            snoozedNotifs++;
            Log.v(TAG, "Written to DB");

            cancelNotification(sbn.getKey());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package not found: " + e.toString());
        } catch (IOException e) {
            Log.w(TAG, "Exception occurred during serialization: " + e.toString());
        }
    }

    private void flushAllSnoozedNotifs() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                RenotifyContract.NotifEntry._ID,
                RenotifyContract.NotifEntry.COLUMN_NAME_NOTIFICATION,
                RenotifyContract.NotifEntry.COLUMN_NAME_PACKAGE
        };

        String selection = RenotifyContract.NotifEntry.COLUMN_NAME_REPOSTED + " = ?";
        String[] selectionArgs = { "0" };

        Cursor cur = null;
        try {
            cur = db.query(
                    RenotifyContract.NotifEntry.TABLE_NAME,   // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                      // The sort order
            );

            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            JsonFactory factory = new JsonFactory();

            List<String> ids = new ArrayList<>();
            if (cur != null && cur.moveToFirst()) {
                Log.v(TAG, "Flushed snoozed notifications: " + cur.getCount());
                int i = 1;
                do {
                    String notif = cur.getString(cur.getColumnIndexOrThrow(RenotifyContract.NotifEntry.COLUMN_NAME_NOTIFICATION));
                    String packageName = cur.getString(cur.getColumnIndexOrThrow(RenotifyContract.NotifEntry.COLUMN_NAME_PACKAGE));
                    try {
                        Context packageContext = createPackageContext(packageName, 0);
                        NotificationConverter converter = new NotificationConverter(packageContext);
                        LoganSquare.registerTypeConverter(PendingIntent.class, new PendingIntentConverter(getApplicationContext()));
                        Notification notification = converter.parse(factory.createParser(notif));
                        notification.extras.putChar("replayed", 'Y');
                        mNotifyMgr.notify(i++, notification);

                        ids.add(cur.getString(cur.getColumnIndexOrThrow(RenotifyContract.NotifEntry._ID)));
                    } catch (IOException e) {
                        Log.w(TAG, "Exception while parsing in converter: " + e.toString());
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, "Package not found. Skipping notification");
                    }
                } while (cur.moveToNext());
            }

            if (ids.size() > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(RenotifyContract.NotifEntry.COLUMN_NAME_REPOSTED, 1);
                selection = RenotifyContract.NotifEntry._ID + " IN (" + ids.toString().substring(1, ids.toString().length()-1) + ")";
                db.update(RenotifyContract.NotifEntry.TABLE_NAME, contentValues, selection, null);
                snoozedNotifs -= ids.size();
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
            db.close();
        }
        updateReminderNotification();
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
        if (!isCriteriaMet()) {
            flushAllSnoozedNotifs();
        }
    }

    public void clearAllNotifs() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(RenotifyContract.NotifEntry.TABLE_NAME, null, null);
        db.close();
        snoozedNotifs = 0;
        cancelAllNotifications();
    }

    public void onPreferenceChanged(String key) {
        onInterruptionFilterChanged(0);
        switch (key) {
            case MainActivity.PREF_KEY_BLOCK_NOTIFICATIONS:
            case MainActivity.PREF_KEY_WORK_HOURS:
            case MainActivity.PREF_KEY_WORK_HOURS_END:
                if(timer != null)
                {
                    timerTask.cancel();
                    timer.cancel();
                    timer.purge();
                    timer = null;
                    timerTask = null;
                }
                if (mPreferences.getBoolean(MainActivity.PREF_KEY_BLOCK_NOTIFICATIONS, false)
                        && mPreferences.getBoolean(MainActivity.PREF_KEY_WORK_HOURS, false))
                {
                    timer = new Timer();
                    timerTask = new MyTimerTask();
                    Date date = new Date();
                    long endTime = mPreferences.getLong(MainActivity.PREF_KEY_WORK_HOURS_END, 0);
                    date.setHours((int) (endTime / 3600000));
                    date.setMinutes((int) (endTime % 3600000 / 60000) - date.getTimezoneOffset());
                    date.setSeconds(0);
                    timer.schedule(timerTask, date , 24*3600*1000);
                }
                break;
            case MainActivity.PREF_SHOW_REMINDER:
                if(mPreferences.getBoolean(MainActivity.PREF_SHOW_REMINDER, false))
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
