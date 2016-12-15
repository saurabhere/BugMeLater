package com.monkapproves.bugmeagain;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.service.notification.NotificationListenerService;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private AppCompatDelegate mDelegate;
    private static final String KEY_PREF_INTERRUPTION_FILTER = "pref_key_interruption_filter_settings";
    private static final Map<Integer, String> mpInterruptionFilter = new HashMap<>();

    static {
        mpInterruptionFilter.put(NotificationListenerService.INTERRUPTION_FILTER_ALL, "All");
        mpInterruptionFilter.put(NotificationListenerService.INTERRUPTION_FILTER_PRIORITY, "Priority");
        mpInterruptionFilter.put(NotificationListenerService.INTERRUPTION_FILTER_NONE, "None");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    private void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        switch (key) {
            case KEY_PREF_INTERRUPTION_FILTER: {
                Preference filterPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                String interruptionFilter = mpInterruptionFilter.get(Integer.parseInt(sharedPreferences.getString(key, "")));
                filterPref.setSummary(interruptionFilter);
                break;
            }
        }
    }

    public void buttonClicked(View v) {
//        if (v.getId() == R.id.btnCreateNotify) {
//            NotificationCompat.Action action1 =
//                    new NotificationCompat.Action.Builder(R.drawable.ic_info_black_24dp,
//                            getString(R.string.app_name1), null)
//                            .build();
//            NotificationCompat.Action action2 =
//                    new NotificationCompat.Action.Builder(R.drawable.ic_info_black_24dp,
//                            getString(R.string.app_name2), null)
//                            .build();
//            NotificationCompat.Builder mBuilder =
//                    new NotificationCompat.Builder(this)
//                            .setSmallIcon(R.drawable.ic_info_black_24dp)
//                            .setContentTitle("My notification number : "+ Math.random())
//                            .setContentText("Hello World!")
//                            .addAction(action1)
//                            .addAction(action2);
//            int mNotificationId = 001;
//            NotificationManager mNotifyMgr =
//                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            // Builds the notification and issues it.
//            mNotifyMgr.notify(mNotificationId, mBuilder.build());
//        } else if (v.getId() == R.id.btnClearNotify) {
//            Intent i = new Intent("com.monkapproves.bugmelater.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
//            i.putExtra("command", "clearAll");
//            sendBroadcast(i);
//        } else if (v.getId() == R.id.btnListNotify) {
//            Intent i = new Intent("com.monkapproves.bugmelater.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
//            i.putExtra("command", "flush");
//            sendBroadcast(i);
//        } else if (v.getId() == R.id.btnGetPermissions) {
//            Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
//            startActivity(intent);
//        }
    }
}
