package net.brainas.android.app.infrustructure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
public class TaskDbHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Brainas.db";

    private static final String COMMA_SEP = ",";

    SQLiteDatabase db;

    /* TABLE TASKS */
    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_NAME_TASKS_ID = "_id";
    public static final String COLUMN_NAME_TASKS_GLOBAL_ID = "global_id";
    public static final String COLUMN_NAME_TASKS_USER = "user";
    public static final String COLUMN_NAME_TASKS_MESSAGE = "message";
    public static final String COLUMN_NAME_TASKS_DESCRIPTION = "description";
    public static final String COLUMN_NAME_TASKS_IMAGE = "image";
    public static final String COLUMN_NAME_TASKS_STATUS = "status";
    private static final String CREATE_TABLE_TASKS =
            "CREATE TABLE " + TABLE_TASKS + " (" +
                    COLUMN_NAME_TASKS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
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
    public static final String COLUMN_NAME_CONDITIONS_ID = "_id";
    public static final String COLUMN_NAME_CONDITIONS_TASK = "task_id";
    public static final String COLUMN_NAME_CONDITIONS_GLOBALID = "global_id";
    private static final String CREATE_TABLE_CONDITIONS =
            "CREATE TABLE " + TABLE_CONDITIONS + " (" +
                    COLUMN_NAME_CONDITIONS_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_CONDITIONS_TASK + " INTEGER," +
                    COLUMN_NAME_CONDITIONS_GLOBALID + " INTEGER" + " )";
    private static final String DELETE_TABLE_CONDITIONS =
            "DROP TABLE IF EXISTS " + TABLE_CONDITIONS;

    /* TABLE EVENTS */
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_NAME_EVENTS_ID = "_id";
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

    public TaskDbHelper(AppDbHelper appDbHelper) {
        this.db = appDbHelper.getDbAccess();
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TASKS);
        db.execSQL(CREATE_TABLE_CONDITIONS);
        db.execSQL(CREATE_TABLE_EVENTS);
        db.execSQL(CREATE_TABLE_EVENT_TYPES);
    }

    public static void onUpgrade(SQLiteDatabase db) {
        db.execSQL(DELETE_TABLE_TASKS);
        db.execSQL(DELETE_TABLE_CONDITIONS);
        db.execSQL(DELETE_TABLE_EVENTS);
        db.execSQL(DELETE_TABLE_EVENT_TYPES);
    }

    public static void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<Task> getTasks(Map<String,Object> params, Integer accountId){
        ArrayList<Task> tasks = new ArrayList<Task>();

        String selection = COLUMN_NAME_TASKS_USER + " LIKE ?";
        List<String> selectionArgsList = new ArrayList<String>();
        selectionArgsList.add(accountId.toString());

        // by group
        if (params != null && params.containsKey("GROUP_OF_TASKS")) {
            TasksManager.GROUP_OF_TASKS group = (TasksManager.GROUP_OF_TASKS)params.get("GROUP_OF_TASKS");
            switch (group) {
                case ACTIVE :
                    selection = selection + " and " + COLUMN_NAME_TASKS_STATUS + " LIKE ?";
                    selectionArgsList.add(group.toString());
                    break;

                case WAITING :
                    selection = selection + " and " + COLUMN_NAME_TASKS_STATUS + " LIKE ?";
                    selectionArgsList.add(group.toString());
                    break;

                case USED :
                    selection = selection + " and (" + COLUMN_NAME_TASKS_STATUS + " LIKE ? OR " + COLUMN_NAME_TASKS_STATUS + " LIKE ?)";
                    selectionArgsList.add("DONE");
                    selectionArgsList.add("CANCELED");
                    break;
            }
        }

        // by local id
        if (params != null && params.containsKey("TASK_ID")) {
            selection = selection + " and " + COLUMN_NAME_TASKS_ID + " LIKE ?";
            selectionArgsList.add(params.get("TASK_ID").toString());
        }

        // by global id
        if (params != null && params.containsKey("TASK_GLOBAL_ID")) {
            selection = selection + " and " + COLUMN_NAME_TASKS_GLOBAL_ID + " LIKE ?";
            selectionArgsList.add(params.get("TASK_GLOBAL_ID").toString());
        }

        String[] selectionArgs = selectionArgsList.toArray(new String[selectionArgsList.size()]);


        //String selectQuery = "";
        //Cursor cursor = db.rawQuery(selectQuery, null);
        Cursor cursor = db.query(
                TABLE_TASKS,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        Task task;
        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_ID) ));
                String message = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_MESSAGE) );

                task = new Task(id, accountId, message);

                String globalIdStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_GLOBAL_ID));
                if (globalIdStr != null) {
                    int globalId = Integer.parseInt(globalIdStr);
                    task.setGlobalId(globalId);
                }

                String status = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_STATUS));
                task.setStatus(status);
                task.setConditions(getConditions(task));
                String description = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_DESCRIPTION));
                task.setDescription(description);
                tasks.add(task);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return tasks;
    }


    public long addOrUpdateTask(Task task) {
        long taskId = 0;

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TASKS_USER, task.getAccountId());
        values.put(COLUMN_NAME_TASKS_MESSAGE, task.getMessage());
        values.put(COLUMN_NAME_TASKS_GLOBAL_ID, task.getGlobalId());
        if (task.getStatus() != null) {
            values.put(COLUMN_NAME_TASKS_STATUS, task.getStatus().toString());
        }
        values.put(COLUMN_NAME_TASKS_DESCRIPTION, task.getDescription());

        int nRowsEffected = 0;
        if (task.getId() != 0) {
            String selection = COLUMN_NAME_TASKS_ID + " LIKE ?";
            String[] selectionArgs = {String.valueOf(task.getId())};
            nRowsEffected = db.update(
                    TABLE_TASKS,
                    values,
                    selection,
                    selectionArgs);
            taskId = task.getId();
        } else if (task.getGlobalId() != 0){
            String selection = COLUMN_NAME_TASKS_GLOBAL_ID + " LIKE ?";
            String[] selectionArgs = {String.valueOf(task.getGlobalId())};
            nRowsEffected = db.update(
                    TABLE_TASKS,
                    values,
                    selection,
                    selectionArgs);
            taskId = getLocalIdByGlobal(task.getGlobalId(), TABLE_TASKS);
        }

        if (nRowsEffected == 0) {
            long newRowId = db.insert(
                    TABLE_TASKS,
                    null,
                    values);
            taskId = newRowId;
        }

        saveConditions(task.getConditions(), task.getId());

        return taskId;
    }

    public boolean deleteTaskById(long taskId) {
        // delete task
        String selection = COLUMN_NAME_TASKS_ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(taskId) };
        db.delete(TABLE_TASKS, selection, selectionArgs);

        // delete conditions
        String selectQuery = "SELECT * FROM " + TABLE_CONDITIONS + " WHERE task_id = " + taskId;
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            int conditionId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CONDITIONS_ID)));

            // delete events
            String selectionForEvents = COLUMN_NAME_EVENTS_CONDITION + " LIKE ?";
            String[] selectionArgsForEvent = { String.valueOf(conditionId) };
            db.delete(TABLE_EVENTS, selectionForEvents, selectionArgsForEvent);
        }
        String selectionForConditions = COLUMN_NAME_CONDITIONS_TASK + " LIKE ?";
        String[]  selectionArgsForConditions = { String.valueOf(taskId) };
        db.delete(TABLE_CONDITIONS, selectionForConditions, selectionArgsForConditions);
        cursor.close();
        return true;
    }

    public long deleteTaskByGlobalId(int taskGlobalId) {
        long localId = getLocalIdByGlobal(taskGlobalId, TABLE_TASKS);
        deleteTaskById(localId);
        return localId;
    }


    private void saveConditions(ArrayList<Condition> conditions, long taskId) {
        long newRowId = 0;
        for (Condition condition : conditions) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_CONDITIONS_TASK, taskId);
            values.put(COLUMN_NAME_CONDITIONS_GLOBALID, condition.getGlobalId());

            int nRowsEffected = 0;
            if (condition.getId() != 0) {
                String selection = COLUMN_NAME_CONDITIONS_ID + " LIKE ?";
                String[] selectionArgs = { String.valueOf(condition.getId()) };
                nRowsEffected = db.update(
                        TABLE_CONDITIONS,
                        values,
                        selection,
                        selectionArgs);
            } else if (condition.getGlobalId() != 0) {
                String selection = COLUMN_NAME_CONDITIONS_GLOBALID + " LIKE ?";
                String[] selectionArgs = { String.valueOf(condition.getGlobalId()) };
                nRowsEffected = db.update(
                        TABLE_CONDITIONS,
                        values,
                        selection,
                        selectionArgs);
                if (nRowsEffected > 0) {
                    condition.setId(getLocalIdByGlobal(condition.getGlobalId(), TABLE_CONDITIONS));
                }
            }

            if (nRowsEffected == 0) {
                newRowId = db.insert(
                        TABLE_CONDITIONS,
                        null,
                        values);
                condition.setId(newRowId);
            }


            saveEvents(condition.getEvents(), condition.getId());
        }
    }

    private void saveEvents(ArrayList<Event> events, long conditionId) {
        long newRowId = 0;
        for (Event event : events) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_EVENTS_GLOBALID, event.getGlobalId());
            values.put(COLUMN_NAME_EVENTS_CONDITION, conditionId);
            values.put(COLUMN_NAME_EVENTS_TYPE, event.getType().toString());
            values.put(COLUMN_NAME_EVENTS_PARAMS, event.getJSONStringWithParams());

            int nRowsEffected = 0;
            if (event.getId() != 0) {
                String selection = COLUMN_NAME_EVENTS_ID + " LIKE ?";
                String[] selectionArgs = { String.valueOf(event.getId()) };

                nRowsEffected = db.update(
                        TABLE_EVENTS,
                        values,
                        selection,
                        selectionArgs);
            } else if (event.getGlobalId() != 0) {
                String selection = COLUMN_NAME_EVENTS_GLOBALID + " LIKE ?";
                String[] selectionArgs = { String.valueOf(event.getGlobalId()) };
                nRowsEffected = db.update(
                        TABLE_EVENTS,
                        values,
                        selection,
                        selectionArgs);
                if (nRowsEffected > 0) {
                    event.setId(getLocalIdByGlobal(event.getGlobalId(), TABLE_CONDITIONS));
                }
            }


            if (nRowsEffected == 0) {
                newRowId = db.insert(
                        TABLE_EVENTS,
                        null,
                        values);
                event.setId(newRowId);
            }
        }
    }

    private ArrayList<Condition> getConditions(Task task) {
        ArrayList<Condition> conditions = new ArrayList<Condition>();
        String selectQuery = "SELECT * FROM " + TABLE_CONDITIONS + " WHERE " + COLUMN_NAME_CONDITIONS_TASK + " = " + task.getId();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CONDITIONS_ID)));
                int globalId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CONDITIONS_GLOBALID)));
                long taskId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CONDITIONS_TASK)));

                Condition condition = new Condition(id, globalId, taskId);
                condition.addEvents(getEvents(condition));
                conditions.add(condition);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return conditions;
    }

    private ArrayList<Event> getEvents(Condition condition) {
        ArrayList<Event> events = new ArrayList<Event>();
        String selectQuery = "SELECT * FROM " + TABLE_EVENTS + " WHERE " + COLUMN_NAME_EVENTS_CONDITION + " = " + condition.getId();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Long id = Long.parseLong(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_EVENTS_ID)));
                int globalId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_EVENTS_GLOBALID)));
                String type = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_EVENTS_TYPE));
                String params = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_EVENTS_PARAMS));

                Event event = null;
                switch (type) {
                    case "GPS" :
                        event = new EventGPS(id, globalId, condition.getId());
                    break;
                }
                event.fillInParamsFromJSONString(params);
                events.add(event);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return events;
    }

    private long getLocalIdByGlobal(long globalId, String tableName) {
        int localId = 1;
        String selectQuery = "SELECT * FROM " + tableName + " WHERE global_id = " + globalId;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            localId = Integer.parseInt(cursor.getString(cursor.getColumnIndex("_id")));
        }

        cursor.close();
        return localId;
    }

    public void close() {
        db.close();
    }
}
