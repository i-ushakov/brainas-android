package net.brainas.android.app.infrustructure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import net.brainas.android.app.domain.models.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 11/12/2015.
 */
public class TaskDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Brainas.db";

    private static final String COMMA_SEP = ",";

    public static final String TABLE_NAME = "tasks";
    public static final String COLUMN_NAME_TASK_ID = "id";
    public static final String COLUMN_NAME_GLOBAL_TASK_ID = "global_id";
    public static final String COLUMN_NAME_USER = "user";
    public static final String COLUMN_NAME_MESSAGE = "message";
    public static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_IMAGE = "image";


    SQLiteDatabase db;

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_TASK_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_GLOBAL_TASK_ID + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_USER + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_MESSAGE + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_DESCRIPTION + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_IMAGE + " INTEGER" + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private static final String[] projection = {
            COLUMN_NAME_TASK_ID,
            COLUMN_NAME_GLOBAL_TASK_ID,
            COLUMN_NAME_USER,
            COLUMN_NAME_MESSAGE,
            COLUMN_NAME_DESCRIPTION,
            COLUMN_NAME_IMAGE
    };

    public TaskDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.db = this.getWritableDatabase();
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public List<Task> getTasks(){
        List<Task> tasks = new ArrayList<Task>();


        /*
        String sortOrder =
                FeedEntry.COLUMN_NAME_UPDATED + " DESC";
                */

        //String selection = COLUMN_NAME_TASK_ID + " LIKE ?";

        //String[] selectionArgs = { String.valueOf(1) };

        Cursor cursor = db.query(
                TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null, //selection,                                // The columns for the WHERE clause
                null, //selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null //sortOrder                                 // The sort order
        );

        Task task = null;
        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASK_ID) ));
                String message = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_MESSAGE) );

                task = new Task(id, message);

                String globalIdStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GLOBAL_TASK_ID));
                if (globalIdStr != null) {
                    int globalId = Integer.parseInt(globalIdStr);
                    task.setGlobalId(globalId);
                }

                //task.setTitle(cursor.getString(1));
                //task.setAuthor(cursor.getString(2));

                // Add book to books
                tasks.add(task);
            } while (cursor.moveToNext());
        }

        return tasks;
    }

    public long addOrUpdateTask(Task task) {
        long newRowId = 0;
        String selection = COLUMN_NAME_GLOBAL_TASK_ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(task.getGlobalId()) };

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_MESSAGE, task.getMessage());
        values.put(COLUMN_NAME_GLOBAL_TASK_ID, task.getGlobalId());

        int nRowsEffected = db.update(
                TABLE_NAME,
                values,
                selection,
                selectionArgs);

        if (nRowsEffected == 0) {
            newRowId = db.insert(
                    TABLE_NAME,
                    null,
                    values);
        }

        return newRowId;
    }
}
