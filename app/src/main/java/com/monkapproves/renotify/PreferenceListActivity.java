package com.monkapproves.renotify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreferenceListActivity extends AppCompatActivity {

    private SharedPreferences mPreferences;

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

    private void loadApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> mApps = getPackageManager().queryIntentActivities(mainIntent, 0);

        ListView listView = (ListView) findViewById(R.id.appListView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBoxView);
                checkBox.setChecked(!checkBox.isChecked());
                setAppPreference(checkBox);
            }
        });
        listView.setAdapter(new AppsAdapter(this, mApps));
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
        private List<ResolveInfo> mApps;

        AppsAdapter(Context context, List<ResolveInfo> mApps) {
            this.inflater = LayoutInflater.from(context);
            this.mApps = mApps;
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
            ResolveInfo info = this.mApps.get(position);
            String packageName = info.activityInfo.packageName;
            handler.iconImage.setImageDrawable(info.loadIcon(getPackageManager()));
            handler.textLabel.setText(info.loadLabel(getPackageManager()));
            handler.checkBox.setChecked(mPreferences.getStringSet("excludeApps", new HashSet<String>())
                    .contains(packageName));
            handler.checkBox.setTag(packageName);

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
