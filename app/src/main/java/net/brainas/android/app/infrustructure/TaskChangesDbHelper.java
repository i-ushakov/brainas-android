package net.brainas.android.app.infrustructure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.Pair;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.models.Task;

import org.json.JSONException;

import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Kit Ushakov on 11/12/2015.
 */
public class TaskChangesDbHelper {

    private static final String COMMA_SEP = ",";

    SQLiteDatabase db;

    /* TABLE TASK_CHANGES */
    public static final String TABLE_TASKS_CHANGES = "tasks_changes";
    public static final String COLUMN_NAME_TASKS_CHANGES_ID = "id";
    public static final String COLUMN_NAME_TASKS_CHANGES_ACCOUNTID = "account_id";
    public static final String COLUMN_NAME_TASKS_CHANGES_TASKID = "task_id";
    public static final String COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID = "task_global_id";
    public static final String COLUMN_NAME_TASKS_CHANGES_STATUS = "status";
    public static final String COLUMN_NAME_TASKS_CHANGES_DATETIME = "date_time";
    public static final String COLUMN_NAME_TASKS_CHANGES_NEEDSYNC = "need_sync";
    private static final String CREATE_TABLE_TASKS_CHANGES =
            "CREATE TABLE " + TABLE_TASKS_CHANGES + " (" +
                    COLUMN_NAME_TASKS_CHANGES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT ," +
                    COLUMN_NAME_TASKS_CHANGES_ACCOUNTID + " INTEGER" + COMMA_SEP +
                    COLUMN_NAME_TASKS_CHANGES_TASKID + " INTEGER ," +
                    COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID + " INTEGER ," +
                    COLUMN_NAME_TASKS_CHANGES_STATUS + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_TASKS_CHANGES_DATETIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" + COMMA_SEP +
                    COLUMN_NAME_TASKS_CHANGES_NEEDSYNC + " INTEGER" + " )";
    private static final String DELETE_TABLE_TASKS_CHANGES =
            "DROP TABLE IF EXISTS " + TABLE_TASKS_CHANGES;

    private static final String[] projection = {
            COLUMN_NAME_TASKS_CHANGES_ID,
            COLUMN_NAME_TASKS_CHANGES_ACCOUNTID,
            COLUMN_NAME_TASKS_CHANGES_TASKID,
            COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID,
            COLUMN_NAME_TASKS_CHANGES_STATUS,
            COLUMN_NAME_TASKS_CHANGES_DATETIME,
            COLUMN_NAME_TASKS_CHANGES_NEEDSYNC
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
        values.put(COLUMN_NAME_TASKS_CHANGES_NEEDSYNC, 1);

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

    public HashMap<Long, Pair<String,String>> getChangedTasks()throws JSONException, ParserConfigurationException {
        return getChangedTasks(false);
    }

    public HashMap<Long, Pair<String,String>> getChangedTasks(boolean allTasks) throws JSONException, ParserConfigurationException {
        HashMap<Long, Pair<String,String>>  changedTasks = new HashMap<Long, Pair<String,String>> ();

        int userAccountId = ((BrainasApp)(BrainasApp.getAppContext())).getAccountsManager().getCurrentAccountId();
        String selection = COLUMN_NAME_TASKS_CHANGES_ACCOUNTID + " LIKE ? ";
        if (!allTasks) {
            selection = selection +  " AND " + COLUMN_NAME_TASKS_CHANGES_NEEDSYNC + " = 1";
        }
        String[] selectionArgs = { String.valueOf(userAccountId) };

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
            String taskStatus = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_STATUS));
            String dateTimeOfChange = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_DATETIME));

            Pair<String, String> change = new Pair(taskStatus, dateTimeOfChange);
            changedTasks.put(taskId, change);
        };

        return changedTasks;
    }

    public int uncheckFromSync(long taskId) {
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(taskId) };

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TASKS_CHANGES_NEEDSYNC, 0);

        int nRowsEffected = db.update(
                TABLE_TASKS_CHANGES,
                values,
                selection,
                selectionArgs);

        return nRowsEffected;
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
