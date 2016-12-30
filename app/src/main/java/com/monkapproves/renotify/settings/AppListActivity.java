package com.monkapproves.renotify.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.monkapproves.renotify.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListActivity extends AppCompatActivity {

    private SharedPreferences mPreferences;

    private static class PInfo {
        String packageName;
        Drawable icon;
        CharSequence label;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_preferencelist);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        loadApps();
    }

    private boolean isSystemPackage(ResolveInfo resolveInfo) {
        return ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    private List<PInfo> getInstalledApps() {
        List<PInfo> installedApps = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(mainIntent, 0);
        PInfo pInfo;
        for (ResolveInfo info : apps) {
            if (!isSystemPackage(info)) {
                pInfo = new PInfo();
                pInfo.packageName = info.activityInfo.packageName;
                pInfo.icon = info.loadIcon(getPackageManager());
                pInfo.label = info.loadLabel(getPackageManager());
                installedApps.add(pInfo);
            }
        }

        return installedApps;
    }

    private void loadApps() {
        List<PInfo> apps = getInstalledApps();

        ListView listView = (ListView) findViewById(R.id.appListView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBoxView);
                checkBox.setChecked(!checkBox.isChecked());
                setAppPreference(checkBox);
            }
        });
        listView.setAdapter(new AppsAdapter(this, apps));
    }

    public void setAppPreference(View view) {
        Set<String> apps = mPreferences.getStringSet("excludeApps", new HashSet<String>());
        if (((CheckBox) view).isChecked()) {
            apps.add((String) view.getTag());
        } else {
            apps.remove((String) view.getTag());
        }
        mPreferences.edit().putStringSet("excludeApps", apps).apply();
    }

    public class AppsAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private List<PInfo> mApps;
        private Set<String> mDefaultExcludeApps = new HashSet<>();

        AppsAdapter(Context context, List<PInfo> mApps) {
            this.inflater = LayoutInflater.from(context);
            this.mDefaultExcludeApps.add(context.getPackageName());
            this.mApps = mApps;
        }

        private boolean isSystemPackage(ResolveInfo resolveInfo) {
            return ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHandler handler;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.preferencelist_item, null);
                handler = new ViewHandler();
                handler.textLabel = (TextView) convertView.findViewById(R.id.textView);
                handler.iconImage = (ImageView)convertView.findViewById(R.id.imageView);
                handler.checkBox = (CheckBox) convertView.findViewById(R.id.checkBoxView);
                convertView.setTag(handler);
            } else {
                handler = (ViewHandler)convertView.getTag();
            }
            PInfo info = this.mApps.get(position);
            handler.iconImage.setImageDrawable(info.icon);
            handler.textLabel.setText(info.label);
            handler.checkBox.setChecked(mPreferences.getStringSet("excludeApps", mDefaultExcludeApps)
                    .contains(info.packageName));
            handler.checkBox.setTag(info.packageName);

            return convertView;

        }

        class ViewHandler {
            TextView textLabel;
            ImageView iconImage;
            CheckBox checkBox;
        }


        public final int getCount() {
            return mApps.size();
        }

        public final Object getItem(int position) {
            return mApps.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }
    }
}
