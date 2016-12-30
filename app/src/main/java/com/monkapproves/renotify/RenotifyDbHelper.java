package com.monkapproves.renotify;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RenotifyDbHelper extends SQLiteOpenHelper {

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RenotifyContract.NotifEntry.TABLE_NAME + " (" +
                    RenotifyContract.NotifEntry._ID + " INTEGER PRIMARY KEY," +
                    RenotifyContract.NotifEntry.COLUMN_NAME_SBN_KEY + " TEXT NOT NULL," +
                    RenotifyContract.NotifEntry.COLUMN_NAME_PACKAGE + " TEXT NOT NULL," +
                    RenotifyContract.NotifEntry.COLUMN_NAME_NOTIFICATION + " TEXT," +
                    RenotifyContract.NotifEntry.COLUMN_NAME_POSTED_ON + " INTEGER," +
                    RenotifyContract.NotifEntry.COLUMN_NAME_REPOSTED + " INTEGER DEFAULT 0)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RenotifyContract.NotifEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Renotify.db";

    public RenotifyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
