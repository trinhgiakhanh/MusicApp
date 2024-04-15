package com.groupproject.music;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class MusicDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "music_database";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_NAME = "songs";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_URI = "uri";
    private static final String COLUMN_ARTWORK_URI = "artwork_uri";
    private static final String COLUMN_SIZE = "size";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_ALBUM = "albumId";

    public MusicDatabaseHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableSql = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_URI + " TEXT, " +
                COLUMN_ARTWORK_URI + " TEXT, " +
                COLUMN_SIZE + " INTEGER, " +
                COLUMN_DURATION + " INTEGER)"; // Added space before "INTEGER"
        db.execSQL(createTableSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addSong(String title, String uri, String artworkUri, int size, int duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_URI, uri);
        values.put(COLUMN_ARTWORK_URI, artworkUri);
        values.put(COLUMN_SIZE, size);
        values.put(COLUMN_DURATION, duration);
        long res = db.insert(TABLE_NAME, null, values);
        db.close();
        return res != -1;
    }


    public int updateSong(long id, String title, String uri, String artworkUri, int size, int duration, int albumId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_URI, uri);
        values.put(COLUMN_ARTWORK_URI, artworkUri);
        values.put(COLUMN_SIZE, size);
        values.put(COLUMN_DURATION, duration);
        values.put(COLUMN_ALBUM, albumId);
        // updating row
        return db.update(TABLE_NAME, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
    }
    public void deleteSong(Uri songUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_URI + "=?";
        String[] whereArgs = { songUri.toString() };
        db.delete(TABLE_NAME, whereClause, whereArgs);
        db.close();
    }
    public Cursor getAllSongs() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null, null, null, null, null, null);
    }
    public boolean isUriExist(Uri uri) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_URI + " = ?";
        String[] selectionArgs = { uri.toString() };
        Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }
    public static String getColumnId() {
        return COLUMN_ID;
    }
    public static String getColumnTitle() {
        return COLUMN_TITLE;
    }
    public static String getColumnUri() {
        return COLUMN_URI;
    }
    public static String getColumnArtworkUri() {
        return COLUMN_ARTWORK_URI;
    }
    public static String getColumnSize() {
        return COLUMN_SIZE;
    }
    public static String getColumnDuration() {
        return COLUMN_DURATION;
    }
    public static String getTableName() {
        return TABLE_NAME;
    }

    // Add other methods as needed for updating, deleting, and querying songs
}