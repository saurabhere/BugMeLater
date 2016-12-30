package com.monkapproves.renotify;

import android.provider.BaseColumns;

public final class RenotifyContract {
    private RenotifyContract() {}

    public static class NotifEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_SBN_KEY = "sbn_key";
        public static final String COLUMN_NAME_PACKAGE = "package";
        public static final String COLUMN_NAME_NOTIFICATION = "notification";
        public static final String COLUMN_NAME_POSTED_ON = "posted_on";
        public static final String COLUMN_NAME_REPOSTED = "reposted";
    }
}
