package net.brainas.android.app;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.multidex.MultiDex;
import android.util.Log;

import net.brainas.android.app.UI.NotificationController;
import net.brainas.android.app.UI.logic.ReminderScreenManager;
import net.brainas.android.app.activities.MainActivity;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.GoogleApiHelper;
import net.brainas.android.app.domain.helpers.NotificationManager;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.LocationProvider;
import net.brainas.android.app.infrustructure.SynchronizationManager;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.infrustructure.UserAccountDbHelper;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class BrainasApp extends Application implements AccountsManager.SingInObserver {
    public static final String BRAINAS_APP_PREFS = "BrainasAppPrefs";

    public static final String TAG = "BrainasApp";
    public static final String BROADCAST_ACTION_APP_VISABILITY_WAS_CHANGED = "net.brainas.android.app.brainasapp.visability_was_changed";

    private static Context context;

    private UserAccount userAccount = null;

    private static boolean appVisible;

    // Activities
    private MainActivity mainActivity;

    // Domain Layer Managers
    private TasksManager tasksManager;
    private ActivationManager activationManager;
    private SynchronizationManager synchronizationManager;
    private NotificationManager notificationManager;
    private TaskHelper taskHelper;

    // UI Layer Managers
    private ReminderScreenManager reminderScreenManager;

    // Infrastructure Layer Managers
    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private UserAccountDbHelper userAccountDbHelper;
    private LocationProvider locationProvider;
    private GoogleApiHelper googleApiHelper;

    // Application Layer Managers
    private AccountsManager accountsManager;

    public void onCreate() {
        super.onCreate();
        BrainasApp.context = getApplicationContext();
        accountsManager = new AccountsManager();
        accountsManager.attach(this);
        appDbHelper = new AppDbHelper(context);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        taskChangesDbHelper = new TaskChangesDbHelper(appDbHelper);
        userAccountDbHelper = new UserAccountDbHelper(appDbHelper);
        locationProvider = new LocationProvider(this);
        googleApiHelper = new GoogleApiHelper(context);
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, accountsManager.getCurrentAccountId());
        taskHelper = new TaskHelper();
        activationManager = new ActivationManager();
        accountsManager.attach(activationManager);
        synchronizationManager = new SynchronizationManager();
        accountsManager.attach(synchronizationManager);
        notificationManager = new NotificationManager();
    }

    public void updateAfterSingIn(UserAccount userAccount) {
        //tasksManager = new TasksManager(taskDbHelper, accountsManager.getCurrentAccountId());
    }

    public void updateAfterSingOut() {
        //tasksManager = new TasksManager(taskDbHelper, null);
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
        return new TasksManager(taskDbHelper, taskChangesDbHelper, accountsManager.getCurrentAccountId());
    }

    public TaskHelper getTaskHelper() {return this.taskHelper;}

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

    public GoogleApiHelper getGoogleApiHelper() {
        return googleApiHelper;
    }

    public LocationProvider getLocationProvider() {
        return this.locationProvider;
    }

    public SynchronizationManager getSynchronizationManager() {
        return synchronizationManager;
    }

    public void setUserAccount(UserAccount userAccount) {
        if (userAccount != null) {
            if (this.userAccount == null || this.userAccount.getLocalAccountId() != userAccount.getLocalAccountId()) {
                this.userAccount = userAccount;
                userAccountDbHelper.updateOrCreate(userAccount);
                userAccount.setLocalAccountId(userAccountDbHelper.getUserAccountId(userAccount.getAccountName()));
                saveLastUsedAccountInPref();
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

    public static boolean isAppVisible() {
        return appVisible;
    }

    public static void activityResumed() {
        appVisible = true;
        notifyAboutSyncronization();
        Log.i(TAG, "App is visable");
        NotificationController.removeActivationNotifications(BrainasApp.getAppContext());
    }

    public static void activityPaused() {
        appVisible = false;
        notifyAboutSyncronization();
        Log.i(TAG, "App is NOT visable");
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void saveLastUsedAccountInPref() {
        SharedPreferences preferences = getAppPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastUsedAccount", userAccount.getAccountName());
        editor.commit();
    }

    private static void notifyAboutSyncronization() {
        Intent intent = new Intent(BROADCAST_ACTION_APP_VISABILITY_WAS_CHANGED);
        intent.putExtra("appVisible", appVisible);
        BrainasApp.getAppContext().sendBroadcast(intent);
    }
}