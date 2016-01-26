package net.brainas.android.app.infrustructure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventGPS;
import net.brainas.android.app.domain.models.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Kit Ushakov on 11/12/2015.
 */
public class AppDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Brainas.db";

    private static final String COMMA_SEP = ",";

    SQLiteDatabase db;

    /* TABLE TASKS */
    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_NAME_TASKS_ID = "id";
    public static final String COLUMN_NAME_TASKS_GLOBAL_ID = "global_id";
    public static final String COLUMN_NAME_TASKS_USER = "user";
    public static final String COLUMN_NAME_TASKS_MESSAGE = "message";
    public static final String COLUMN_NAME_TASKS_DESCRIPTION = "description";
    public static final String COLUMN_NAME_TASKS_IMAGE = "image";
    public static final String COLUMN_NAME_TASKS_STATUS = "status";
    private static final String CREATE_TABLE_TASKS =
            "CREATE TABLE " + TABLE_TASKS + " (" +
                    COLUMN_NAME_TASKS_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_TASKS_GLOBAL_ID + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_TASKS_USER + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_TASKS_MESSAGE + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_TASKS_DESCRIPTION + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_TASKS_IMAGE + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_TASKS_STATUS + " TEXT" + " )";
    private static final String DELETE_TABLE_TASKS =
            "DROP TABLE IF EXISTS " + TABLE_TASKS;

    /* TABLE CONDITIONS */
    public static final String TABLE_CONDITIONS = "conditions";
    public static final String COLUMN_NAME_CONDITIONS_ID = "id";
    public static final String COLUMN_NAME_CONDITIONS_TASK = "task_id";
    public static final String COLUMN_NAME_CONDITIONS_GLOBALID = "global_id";
    private static final String CREATE_TABLE_CONDITIONS =
            "CREATE TABLE " + TABLE_CONDITIONS + " (" +
                    COLUMN_NAME_CONDITIONS_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_CONDITIONS_TASK + " INTEGER," +
                    COLUMN_NAME_CONDITIONS_GLOBALID + " INTEGER UNIQUE" + " )";
    private static final String DELETE_TABLE_CONDITIONS =
            "DROP TABLE IF EXISTS " + TABLE_CONDITIONS;

    /* TABLE EVENTS */
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_NAME_EVENTS_ID = "id";
    public static final String COLUMN_NAME_EVENTS_GLOBALID = "global_id";

    public static final String COLUMN_NAME_EVENTS_CONDITION = "condition_id";
    public static final String COLUMN_NAME_EVENTS_TYPE = "type";
    public static final String COLUMN_NAME_EVENTS_PARAMS = "params";
    private static final String CREATE_TABLE_EVENTS =
            "CREATE TABLE " + TABLE_EVENTS + " (" +
                    COLUMN_NAME_EVENTS_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_EVENTS_GLOBALID + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_EVENTS_CONDITION + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_EVENTS_TYPE + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_EVENTS_PARAMS + " TEXT" + " )";
    private static final String DELETE_TABLE_EVENTS =
            "DROP TABLE IF EXISTS " + TABLE_EVENTS;


    /* TABLE EVENT_TYPES */
    public static final String TABLE_EVENT_TYPES = "event_types";
    public static final String COLUMN_NAME_EVENT_TYPES_ID = "id";
    public static final String COLUMN_NAME_EVENT_TYPES_NAME = "name";
    private static final String CREATE_TABLE_EVENT_TYPES =
            "CREATE TABLE " + TABLE_EVENT_TYPES + " (" +
                    COLUMN_NAME_EVENT_TYPES_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_EVENT_TYPES_NAME + " INTEGER" + " )";
    private static final String DELETE_TABLE_EVENT_TYPES =
            "DROP TABLE IF EXISTS " + TABLE_EVENT_TYPES;




    private static final String[] projection = {
            COLUMN_NAME_TASKS_ID,
            COLUMN_NAME_TASKS_GLOBAL_ID,
            COLUMN_NAME_TASKS_USER,
            COLUMN_NAME_TASKS_MESSAGE,
            COLUMN_NAME_TASKS_DESCRIPTION,
            COLUMN_NAME_TASKS_IMAGE,
            COLUMN_NAME_TASKS_STATUS
    };

    public AppDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.db = this.getWritableDatabase();
    }

    public void onCreate(SQLiteDatabase db) {
        TaskDbHelper.onCreate(db);
        UserAccountDbHelper.onCreate(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TaskDbHelper.onUpgrade(db);
        UserAccountDbHelper.onUpgrade(db);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TaskDbHelper.onDowngrade(db, oldVersion, newVersion);
        UserAccountDbHelper.onDowngrade(db, oldVersion, newVersion);
        onUpgrade(db, oldVersion, newVersion);
    }

    public SQLiteDatabase getDbAccess() {
        return db;
    }
}
