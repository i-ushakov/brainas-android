package net.brainas.android.app.infrustructure;

import android.content.ContentValues;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Kit Ushakov on 11/12/2015.
 */
public class UserAccountDbHelper {

    private static final String COMMA_SEP = ",";

    SQLiteDatabase db;

    /* TABLE USER ACCOUNTS */
    public static final String TABLE_USER_ACCOUNTS = "user_accounts";
    public static final String COLUMN_NAME_USER_ACCOUNTS_ID = "id";
    public static final String COLUMN_NAME_USER_ACCOUNTS_EMAIL = "email";
    public static final String COLUMN_NAME_USER_ACCOUNTS_PERSON_NAME = "person_name";
    public static final String COLUMN_NAME_USER_ACCOUNTS_ACCESS_CODE = "access_code";
    public static final String COLUMN_NAME_USER_ACCOUNTS_LAST_VISIT = "last_visit";
    public static final String COLUMN_NAME_USER_ACCOUNTS_CREATED = "created";
    private static final String CREATE_TABLE_USER_ACCOUNTS =
            "CREATE TABLE " + TABLE_USER_ACCOUNTS + " (" +
                    COLUMN_NAME_USER_ACCOUNTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT ," +
                    COLUMN_NAME_USER_ACCOUNTS_EMAIL + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_USER_ACCOUNTS_PERSON_NAME + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_USER_ACCOUNTS_ACCESS_CODE + " TEXT" + COMMA_SEP +
                    COLUMN_NAME_USER_ACCOUNTS_LAST_VISIT + " DATETIME DEFAULT CURRENT_TIMESTAMP" + COMMA_SEP +
                    COLUMN_NAME_USER_ACCOUNTS_CREATED + " DATETIME" + " )";
    private static final String DELETE_TABLE_USER_ACCOUNTS =
            "DROP TABLE IF EXISTS " + TABLE_USER_ACCOUNTS;

    private static final String[] projection = {
            COLUMN_NAME_USER_ACCOUNTS_ID,
            COLUMN_NAME_USER_ACCOUNTS_EMAIL,
            COLUMN_NAME_USER_ACCOUNTS_PERSON_NAME,
            COLUMN_NAME_USER_ACCOUNTS_ACCESS_CODE,
            COLUMN_NAME_USER_ACCOUNTS_LAST_VISIT,
            COLUMN_NAME_USER_ACCOUNTS_CREATED
    };

    public UserAccountDbHelper(AppDbHelper appDbHelper) {
        this.db = appDbHelper.getDbAccess();
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER_ACCOUNTS);
    }

    public static void onUpgrade(SQLiteDatabase db) {
        db.execSQL(DELETE_TABLE_USER_ACCOUNTS);
        onCreate(db);
    }

    public static void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long updateOrCreate(UserAccount userAccount) {
        long  newAccountWasAdded = 0;
        String email = userAccount.getAccountName();
        String selection = COLUMN_NAME_USER_ACCOUNTS_EMAIL + " LIKE ?";
        String[] selectionArgs = { String.valueOf(email) };

        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME_USER_ACCOUNTS_EMAIL, email);
        values.put(COLUMN_NAME_USER_ACCOUNTS_PERSON_NAME, userAccount.getPersonName());
        if(userAccount.getAccessCode() != null) {
            values.put(COLUMN_NAME_USER_ACCOUNTS_ACCESS_CODE, userAccount.getAccessCode());
        }

        int nRowsEffected = db.update(
                TABLE_USER_ACCOUNTS,
                values,
                selection,
                selectionArgs);

        values.put(COLUMN_NAME_USER_ACCOUNTS_CREATED, getDateTime());
        if (nRowsEffected == 0) {
            newAccountWasAdded = db.insert(
                    TABLE_USER_ACCOUNTS,
                    null,
                    values);
        }

        return newAccountWasAdded;
    }

    public int getUserAccountId(String accountName) {
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
            String accessCode = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_USER_ACCOUNTS_ACCESS_CODE));
            userAccount.setAccessCode(accessCode);

            return userAccount;
        }
        return null;
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
