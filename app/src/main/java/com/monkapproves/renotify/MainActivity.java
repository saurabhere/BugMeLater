package com.monkapproves.renotify;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.monkapproves.renotify.welcome.IntroActivity;

public class MainActivity extends AppCompatActivity {
    SharedPreferences mPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isFirstRun = mPreferences.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            //show start activity
            startActivity(new Intent(this, IntroActivity.class));
            mPreferences
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();
        }
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mPreferences.getBoolean("isFirstRun", true)) {
            checkNotificationAccess();
        }
    }

    private void checkNotificationAccess() {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getApplicationContext().getPackageName();

        if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName)) {
            Toast.makeText(this, getString(R.string.toast_provide_access), Toast.LENGTH_LONG).show();
            //DO SOME FLASHY STUFF TO THE UI.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent myIntent = new Intent(this, com.monkapproves.renotify.settings.MainActivity.class);
                this.startActivity(myIntent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
