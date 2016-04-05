package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Kit Ushakov on 11/12/2015.
 */
public class AppDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Brainas.db";

    private static final String COMMA_SEP = ",";

    SQLiteDatabase db;

    public AppDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.db = this.getWritableDatabase();
    }

    public void onCreate(SQLiteDatabase db) {
        TaskDbHelper.onCreate(db);
        UserAccountDbHelper.onCreate(db);
        TaskChangesDbHelper.onCreate(db);
        ServicesDbHelper.onCreate(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TaskDbHelper.onUpgrade(db);
        UserAccountDbHelper.onUpgrade(db);
        TaskChangesDbHelper.onUpgrade(db);
        ServicesDbHelper.onUpgrade(db);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TaskDbHelper.onDowngrade(db, oldVersion, newVersion);
        UserAccountDbHelper.onDowngrade(db, oldVersion, newVersion);
        TaskChangesDbHelper.onDowngrade(db, oldVersion, newVersion);
        ServicesDbHelper.onDowngrade(db, oldVersion, newVersion);
        onUpgrade(db, oldVersion, newVersion);
    }

    public SQLiteDatabase getDbAccess() {
        return db;
    }
}
