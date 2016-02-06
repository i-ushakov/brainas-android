package net.brainas.android.app.infrustructure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.models.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

    public void loggingChanges(Task task) {
        String selection = COLUMN_NAME_TASKS_CHANGES_TASKID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(task.getId()) };

        ContentValues values = new ContentValues();
        int userAccountId = ((BrainasApp)(BrainasApp.getAppContext())).getAccountsManager().getCurrenAccountId();
        values.put(COLUMN_NAME_TASKS_CHANGES_ACCOUNTID, userAccountId);
        values.put(COLUMN_NAME_TASKS_CHANGES_TASKID, task.getId());
        values.put(COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID, task.getGlobalId());
        values.put(COLUMN_NAME_TASKS_CHANGES_STATUS, "UPDATE");
        values.put(COLUMN_NAME_TASKS_CHANGES_DATETIME, Utils.getDateTimeGMT());
        values.put(COLUMN_NAME_TASKS_CHANGES_NEEDSYNC, task.getId());

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

    public JSONArray getAllTasksChanges() throws JSONException {
        JSONArray allTasksChanges = new JSONArray();

        int userAccountId = ((BrainasApp)(BrainasApp.getAppContext())).getAccountsManager().getCurrenAccountId();
        String selection = COLUMN_NAME_TASKS_CHANGES_ACCOUNTID + " LIKE ?";
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

        if (cursor.moveToFirst()) {
            JSONObject change = new JSONObject();
            long taskId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_TASKID)));
            long taskGlobalId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_TASKGLOBALID)));
            String taskStatus = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_STATUS));
            String dateTimeOfChange = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASKS_CHANGES_DATETIME));
            change.put("taskId", taskId);
            change.put("globalId", taskGlobalId);
            change.put("taskStatus", taskStatus);
            change.put("dateTimeOfChange", dateTimeOfChange);
            allTasksChanges.put(change);
        } while (cursor.moveToNext());

        return allTasksChanges;
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
            userAccount.setAccountId(accountId);
            userAccount.setPersonName(personName);

            return userAccount;
        }
        return null;
    }*/
}
