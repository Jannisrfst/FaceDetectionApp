package com.example.facedetectionapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BiometricData.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_DATA = "DATA";
    public static final String COLUMN_PERSON_ID = "PERSON_ID";
    public static final String COLUMN_VECTOR_X = "VECTOR_X";
    public static final String COLUMN_VECTOR_Y = "VECTOR_Y";
    public static final String COLUMN_VECTOR_Z = "VECTOR_Z";
    public static final String COLUMN_NUMMERISCHE_WERTE = "NUMMERISCHE_WERTE";

    private static final String CREATE_TABLE_DATA = "CREATE TABLE " + TABLE_DATA + " (" +
            COLUMN_PERSON_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_VECTOR_X + " FLOAT, " +
            COLUMN_VECTOR_Y + " FLOAT, " +
            COLUMN_VECTOR_Z + " FLOAT, " +
            COLUMN_NUMMERISCHE_WERTE + " DOUBLE" +
            ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int version = oldVersion + 1; version <= newVersion; version++) {
            switch (version) {
                case 2:
                    db.execSQL("ALTER TABLE " + TABLE_DATA + " ADD COLUMN " + COLUMN_NUMMERISCHE_WERTE + " DOUBLE;");
                    break;
            }
        }
    }
}
