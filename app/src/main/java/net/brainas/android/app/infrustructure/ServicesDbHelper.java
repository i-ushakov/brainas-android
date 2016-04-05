package net.brainas.android.app.infrustructure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Kit Ushakov on 11/12/2015.
 */
public class ServicesDbHelper {

    private static final String COMMA_SEP = ",";

    SQLiteDatabase db;

    /* TABLE USER ACCOUNTS */
    public static final String TABLE_SERVICES = "services";
    public static final String COLUMN_NAME_SERVICES_NAME = "name";
    public static final String COLUMN_NAME_SERVICES_PARAMS = "params";
    public static final String COLUMN_NAME_USERVICES_DATETIME = "datetime";

    private static final String CREATE_TABLE_TABLE_SERVICES =
            "CREATE TABLE " + TABLE_SERVICES + " (" +
                    COLUMN_NAME_SERVICES_NAME + " TEXT PRIMARY KEY" + COMMA_SEP +
                    COLUMN_NAME_SERVICES_PARAMS + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_USERVICES_DATETIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" + " )";

    private static final String DELETE_TABLE_TABLE_SERVICES =
            "DROP TABLE IF EXISTS " + TABLE_SERVICES;

    private static final String[] projection = {
            COLUMN_NAME_SERVICES_NAME,
            COLUMN_NAME_SERVICES_PARAMS,
            COLUMN_NAME_USERVICES_DATETIME
    };

    public ServicesDbHelper(AppDbHelper appDbHelper) {
        this.db = appDbHelper.getDbAccess();
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TABLE_SERVICES);
    }

    public static void onUpgrade(SQLiteDatabase db) {
        db.execSQL(DELETE_TABLE_TABLE_SERVICES);
        onCreate(db);
    }

    public static void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

   public void saveServiceParams(String serviceName, String params) {
        String selection = COLUMN_NAME_SERVICES_NAME + " LIKE ?";
        String[] selectionArgs = { String.valueOf(serviceName) };

        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME_SERVICES_NAME, serviceName);
        values.put(COLUMN_NAME_SERVICES_PARAMS, params);

        int nRowsEffected = db.update(
                TABLE_SERVICES,
                values,
                selection,
                selectionArgs);

        if (nRowsEffected == 0) {
            db.insert(TABLE_SERVICES, null, values);
        }
    }

    public String getServiceParams(String serviceName) {
        String params = null;
        String selection = COLUMN_NAME_SERVICES_NAME + " LIKE ?";
        String[] selectionArgs = { String.valueOf(serviceName) };

        Cursor cursor = db.query(
                TABLE_SERVICES,
                projection,
                selection,
                selectionArgs,                  // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );

        if (cursor.moveToFirst()) {
            params = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_SERVICES_PARAMS));
        }

        return params;
    }

    public void addServiceParam(String serviceName, String paramName, String paramValue) {
        try {
            JSONObject currentServiceParams = new JSONObject(getServiceParams(serviceName));
            currentServiceParams.put(paramName, paramValue);
            saveServiceParams(serviceName, currentServiceParams.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
     /* public int getUserAccountId(String accountName) {
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
    }

    public void close() {
        db.close();
    }

    public UserAccount retrieveUserAccountFromDB(String accountName) {
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
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }*/
}
