package net.brainas.android.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDex;

import net.brainas.android.app.UI.logic.ReminderScreenManager;
import net.brainas.android.app.activities.MainActivity;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.NotificationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.Synchronization;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.infrustructure.UserAccountDbHelper;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class BrainasApp extends Application {
    public static final String BRAINAS_APP_PREFS = "BrainasAppPrefs";

    private static Context context;

    private UserAccount userAccount = null;

    // Activities
    private MainActivity mainActivity;

    // Domain Layer Managers
    private TasksManager tasksManager;
    private ActivationManager activationManager;
    private NotificationManager notificationManager;

    // UI Layer Managers
    private ReminderScreenManager reminderScreenManager;

    // Infrastructure Layer Managers
    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private UserAccountDbHelper userAccountDbHelper;

    // Application Layer Managers
    private AccountsManager accountsManager;

    public void onCreate() {
        super.onCreate();
        BrainasApp.context = getApplicationContext();
        accountsManager = new AccountsManager();
        AppDbHelper appDbHelper = new AppDbHelper(context);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        taskChangesDbHelper = new TaskChangesDbHelper(appDbHelper);
        userAccountDbHelper = new UserAccountDbHelper(appDbHelper);
        tasksManager = new TasksManager(taskDbHelper);
        activationManager = new ActivationManager(tasksManager);
        notificationManager = new NotificationManager();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static Context getAppContext() {
        return BrainasApp.context;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public MainActivity getMainActivity() {
        return this.mainActivity;
    }

    public TasksManager getTasksManager() {
        return this.tasksManager;
    }

    public void setReminderScreenManager(ReminderScreenManager reminderScreenManager) {
        this.reminderScreenManager = reminderScreenManager;
    }

    public ReminderScreenManager getReminderScreenManager() {
        return reminderScreenManager;
    }

    public NotificationManager getNotificationManager(){ return this.notificationManager; }

    public TaskDbHelper getTaskDbHelper() {
        return taskDbHelper;
    }

    public TaskChangesDbHelper getTasksChangesDbHelper() {
        return taskChangesDbHelper;
    }

    public UserAccountDbHelper getUserAccountDbHelper() {
        return userAccountDbHelper;
    }

    public ActivationManager getActivationManager() {
        return activationManager;
    }

    public void setUserAccount(UserAccount userAccount) {
        if (userAccount != null) {
            if (this.userAccount == null || this.userAccount.getLocalAccountId() != userAccount.getLocalAccountId()) {
                this.userAccount = userAccount;
                userAccountDbHelper.updateOrCreate(userAccount);
                userAccount.setLocalAccountId(userAccountDbHelper.getUserAccountId(userAccount.getAccountName()));
                saveLastUsedAccountInPref();
                Synchronization.getInstance().stopSynchronization();
                Synchronization.getInstance().startSynchronization(userAccount);
            }
        } else {
            this.userAccount = null;
        }
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public UserAccount getLastUsedAccount() {
        UserAccount userAccount = null;
        SharedPreferences preferences = getAppPreferences();
        String accountName = preferences.getString("lastUsedAccount", null);
        if (accountName != null) {
            userAccount = userAccountDbHelper.retrieveUserAccountFromDB(accountName);
            return userAccount;
        } else {
            return null;
        }
    }

    public SharedPreferences getAppPreferences() {
        SharedPreferences preferences = getSharedPreferences(BRAINAS_APP_PREFS, 0);
        return preferences;
    }

    public AccountsManager getAccountsManager() {
        return this.accountsManager;
    }

    private void saveLastUsedAccountInPref() {
        SharedPreferences preferences = getAppPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastUsedAccount", userAccount.getAccountName());
        editor.commit();
    }
}