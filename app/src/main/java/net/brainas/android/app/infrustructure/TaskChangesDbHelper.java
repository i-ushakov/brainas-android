package net.brainas.android.app.infrustructure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.Pair;
import android.util.Log;

import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.models.Task;

import org.json.JSONException;

import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Kit Ushakov on 11/12/2015.
 */
public class TaskChangesDbHelper {

    private static final String TASK_CHANGES_TAG = "TASK_CHANGES";
    private static final String COMMA_SEP = ",";

    SQLiteDatabase db;

    public static final int ALREADY_SYNCHRONIZED = 0;
    public static final int NEED_FOR_SYNC = 1;
    public static final int SENDING = 2;
    public static final int WAS_CHANGED_WHILE_SYNC = 3;

    /* TABLE TASK_CHANGES */
    public static final String TABLE_TASKS_CHANGES = "tasks_changes";
    public static final String COLUMN_NAME_TASKS_CHANGES_ID = "id";
    public static final String COLUMN_NAME_TASKS_CHANGES_ACCOUNTID = "account_id";
    public static final String COLUMN_NAME_TASKS_CHANGES_TASKID = "task_id";
    public static final String COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID = "task_global_id";
    public static final String COLUMN_NAME_TASKS_CHANGES_STATUS = "status";
    public static final String COLUMN_NAME_TASKS_CHANGES_DATETIME = "date_time";
    public static final String COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS = "need_sync";
    private static final String CREATE_TABLE_TASKS_CHANGES =
            "CREATE TABLE " + TABLE_TASKS_CHANGES + " (" +
                    COLUMN_NAME_TASKS_CHANGES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT ," +
                    COLUMN_NAME_TASKS_CHANGES_ACCOUNTID + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_TASKS_CHANGES_TASKID + " INTEGER ," +
                    COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID + " INTEGER ," +
                    COLUMN_NAME_TASKS_CHANGES_STATUS + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_TASKS_CHANGES_DATETIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" + COMMA_SEP +
                    COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS + " INTEGER" + " )";
    private static final String DELETE_TABLE_TASKS_CHANGES =
            "DROP TABLE IF EXISTS " + TABLE_TASKS_CHANGES;

    private static final String[] projection = {
            COLUMN_NAME_TASKS_CHANGES_ID,
            COLUMN_NAME_TASKS_CHANGES_ACCOUNTID,
            COLUMN_NAME_TASKS_CHANGES_TASKID,
            COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID,
            COLUMN_NAME_TASKS_CHANGES_STATUS,
            COLUMN_NAME_TASKS_CHANGES_DATETIME,
            COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS
    };

    public TaskChangesDbHelper(AppDbHelper appDbHelper) {
        this.db = appDbHelper.getDbAccess();
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TASKS_CHANGES);
    }

    public static void onUpgrade(SQLiteDatabase db) {
        db.execSQL(DELETE_TABLE_TASKS_CHANGES);
        onCreate(db);
    }

    public static void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void loggingChanges(Task task, Integer accountId) {
        loggingChanges(task, accountId, "UPDATED");
    }

    public void loggingChanges(Task task, Integer accountId, String status) {
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(task.getId()) };

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TASKS_CHANGES_ACCOUNTID, accountId);
        values.put(COLUMN_NAME_TASKS_CHANGES_TASKID, task.getId());
        values.put(COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID, task.getGlobalId());
        values.put(COLUMN_NAME_TASKS_CHANGES_STATUS, status);
        values.put(COLUMN_NAME_TASKS_CHANGES_DATETIME, Utils.getDateTimeGMT());
        Integer statusOfChange = getCurrentStatus(task.getId());
        if (statusOfChange == null || statusOfChange == ALREADY_SYNCHRONIZED) {
            values.put(COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS, NEED_FOR_SYNC);
            Log.i(TASK_CHANGES_TAG, "Logging change for task_id " + task.getId() + "with status: " + NEED_FOR_SYNC);
        } else if(statusOfChange == SENDING) {
            values.put(COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS, WAS_CHANGED_WHILE_SYNC);
            Log.i(TASK_CHANGES_TAG, "Logging change for task_id " + task.getId() + "with status: " + WAS_CHANGED_WHILE_SYNC);
        }

        int nRowsEffected = db.update(
                TABLE_TASKS_CHANGES,
                values,
                selection,
                selectionArgs);

        if (nRowsEffected == 0) {
            values.put(COLUMN_NAME_TASKS_CHANGES_STATUS, "CREATED");
            db.insert(
                    TABLE_TASKS_CHANGES,
                    null,
                    values);
        }
    }

    public int setStatusToSending(Long[] tasksIds) {
        if (tasksIds.length == 0) {
            return 0;
        }
        String[] tasksIdsInStr = new String[tasksIds.length];

        for (int i=0; i<tasksIds.length; i++)
            tasksIdsInStr[i] = tasksIds[i] != null ? tasksIds[i].toString() : null;

        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " IN (" + makePlaceholders(tasksIds.length) + ")";
        String[] selectionArgs = tasksIdsInStr;

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TASKS_CHANGES_STATUS, SENDING);

        int nRowsEffected = db.update(
                TABLE_TASKS_CHANGES,
                values,
                selection,
                selectionArgs);

        if (nRowsEffected > 0) {
            Log.i(TASK_CHANGES_TAG, "Chages of Tasks with ids = " + tasksIds.toString() + " was sent for sync with server");
        }
        return nRowsEffected;
    }

    public HashMap<Long, Pair<String,String>> getChangesOfTasks(int accountId)throws JSONException, ParserConfigurationException {
        return getChangesOfTasks(false, accountId);
    }

    public HashMap<Long, Pair<String,String>> getChangesOfTasks(boolean allTasks, int accountId) throws JSONException, ParserConfigurationException {
        HashMap<Long, Pair<String,String>>  changedTasks = new HashMap<Long, Pair<String,String>> ();

        String selection = COLUMN_NAME_TASKS_CHANGES_ACCOUNTID + " LIKE ? ";
        if (!allTasks) {
            selection = selection +  " AND ("
                    + COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS + " = " + NEED_FOR_SYNC + " OR "
                    + COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS + " = " + WAS_CHANGED_WHILE_SYNC + ")";
        }
        String[] selectionArgs = { String.valueOf(accountId) };

        Cursor cursor = db.query(
                TABLE_TASKS_CHANGES,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        while (cursor.moveToNext()) {
            long taskId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_TASKID)));
            String changeStatus = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_STATUS));
            String dateTimeOfChange = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_DATETIME));

            Pair<String, String> change = new Pair(changeStatus, dateTimeOfChange);
            changedTasks.put(taskId, change);
            Log.i(TASK_CHANGES_TAG, "We have change for task_id = " + taskId + ", time: " + dateTimeOfChange + "status: " + changeStatus);
        };

        return changedTasks;
    }

    public int uncheckFromSync(long taskId) {
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(taskId) };

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS, 0);

        int nRowsEffected = db.update(
                TABLE_TASKS_CHANGES,
                values,
                selection,
                selectionArgs);

        return nRowsEffected;
    }

    public int setStatusAfterSync(long taskId) {
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(taskId) };

        ContentValues values = new ContentValues();
        Integer statusOfChange = getCurrentStatus(taskId);
        if (statusOfChange == null || statusOfChange != WAS_CHANGED_WHILE_SYNC) {
            values.put(COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS, ALREADY_SYNCHRONIZED);
            Log.i(TASK_CHANGES_TAG, "Set status " + ALREADY_SYNCHRONIZED + " for task_id = " + taskId + "after sync");
        }  else {
            values.put(COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS, NEED_FOR_SYNC);
            Log.i(TASK_CHANGES_TAG, "Set status " + NEED_FOR_SYNC + " for task_id = " + taskId + "after sync");
        }


        int nRowsEffected = db.update(
                TABLE_TASKS_CHANGES,
                values,
                selection,
                selectionArgs);

        return nRowsEffected;
    }

    public int removeAllSendingStatus(int accountId) {
        String selection = COLUMN_NAME_TASKS_CHANGES_ACCOUNTID + " LIKE ? ";
        selection = selection +  " AND " + COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS + " = " + SENDING;
        String[] selectionArgs = { String.valueOf(accountId) };

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS, ALREADY_SYNCHRONIZED);
        int nRowsEffected0 = db.update(
                TABLE_TASKS_CHANGES,
                values,
                selection,
                selectionArgs);
        Log.i(TASK_CHANGES_TAG, "Removed status " + WAS_CHANGED_WHILE_SYNC + " and set status = " + ALREADY_SYNCHRONIZED + "for " + nRowsEffected0 + " tasks");

        selection = COLUMN_NAME_TASKS_CHANGES_ACCOUNTID + " LIKE ? " +  " AND " + COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS + " = " + WAS_CHANGED_WHILE_SYNC;

        values.put(COLUMN_NAME_TASKS_CHANGES_SYNCSTATUS, NEED_FOR_SYNC);
        int nRowsEffected1 = db.update(
                TABLE_TASKS_CHANGES,
                values,
                selection,
                selectionArgs);

        Log.i(TASK_CHANGES_TAG, "Removed status " + WAS_CHANGED_WHILE_SYNC + " and set status = " + NEED_FOR_SYNC + "for " + nRowsEffected1 + " tasks");

        return nRowsEffected0 + nRowsEffected1;
    }

    public String getTimeOfLastChanges(long localTaskId) {
        String timeOfLastChanges = null;
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(localTaskId) };

        Cursor cursor = db.query(
                TABLE_TASKS_CHANGES,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        if (cursor.moveToFirst()) {
            timeOfLastChanges = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_DATETIME));
        } while (cursor.moveToNext());

        return timeOfLastChanges;
    }

    public void deleteTaskChangesById(long taskId) {
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(taskId) };
        db.delete(TABLE_TASKS_CHANGES, selection, selectionArgs);
    }

    public long getGlobalIdOfDeletedTask(long localTaskId) {
        long globalId = 0;
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(localTaskId) };

        Cursor cursor = db.query(
                TABLE_TASKS_CHANGES,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        if (cursor.moveToFirst()) {
            globalId = Long.parseLong(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID)));
        } while (cursor.moveToNext());

        return globalId;
    }

    public void removeFromSync(long taskGlobalId) {
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(taskGlobalId) };
        db.delete(TABLE_TASKS_CHANGES, selection, selectionArgs);
    }

    /*public int getUserAccountId(String accountName) {
        int accountId = 0;
        String email = accountName;
        String selection = COLUMN_NAME_USER_ACCOUNTS_EMAIL + " LIKE ?";
        String[] selectionArgs = { String.valueOf(email) };

        Cursor cursor = db.query(
                TABLE_USER_ACCOUNTS,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        if (cursor.moveToFirst()) {
            accountId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_USER_ACCOUNTS_ID)));
            return accountId;
        }

        return accountId;
    }*/

    public void close() {
        db.close();
    }

    private Integer getCurrentStatus(long taskId) {
        Integer syncStatus = null;
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ? ";
        String[] selectionArgs = { String.valueOf(taskId) };

        Cursor cursor = db.query(
                TABLE_TASKS_CHANGES,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        while (cursor.moveToNext()) {
            syncStatus = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_STATUS));
            Log.i(TASK_CHANGES_TAG, "Task_id: = " + taskId + " have syncStatus: " + syncStatus);
        };

        return syncStatus;
    }

    private String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    /*public UserAccount retrieveUserAccountFromDB(String accountName) {
        UserAccount userAccount;

        String email = accountName;
        String selection = COLUMN_NAME_USER_ACCOUNTS_EMAIL + " LIKE ?";
        String[] selectionArgs = { String.valueOf(email) };

        Cursor cursor = db.query(
                TABLE_USER_ACCOUNTS,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        if (cursor.moveToFirst()) {
            userAccount = new UserAccount(accountName);
            int accountId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_USER_ACCOUNTS_ID)));
            String personName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_USER_ACCOUNTS_PERSON_NAME));
            userAccount.setLocalAccountId(accountId);
            userAccount.setPersonName(personName);

            return userAccount;
        }
        return null;
    }*/
}
