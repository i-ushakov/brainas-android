package net.brainas.android.app;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

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
import net.brainas.android.app.infrustructure.synchronization.SynchronizationManager;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.infrustructure.UserAccountDbHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.fabric.sdk.android.Fabric;

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

    private Tracker mTracker;

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

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    /**
     * This is a subclass of {@link Application} used to provide shared objects for this app, such as
     * the {@link Tracker}.
     */
    public class AnalyticsApplication extends Application {
        private Tracker mTracker;

        /**
         * Gets the default {@link Tracker} for this {@link Application}.
         * @return tracker
         */
        synchronized public Tracker getDefaultTracker() {
            if (mTracker == null) {
                GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
                // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
                mTracker = analytics.newTracker(R.xml.global_tracker);
            }
            return mTracker;
        }
    }

    private static Thread.UncaughtExceptionHandler mDefaultUEH;
    private static Thread.UncaughtExceptionHandler mCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            CLog.e("BA_UNCAUGHT_EXCEPTION", "uncaughtException", ex);
            mDefaultUEH.uncaughtException(thread, ex);
        }
    };

    public void onCreate() {
        super.onCreate();
        // Order is important!
        // First, start Crashlytics
        Fabric.with(getApplicationContext(), new Crashlytics());

        // Second, set custom UncaughtExceptionHandler
        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler);

        CLog.init(getBaseContext());

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
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, accountsManager.getCurrentAccountId());
    }

    public void updateAfterSingOut() {
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, null);
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
        return tasksManager;
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

    public void setLocationProvider (LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    public SynchronizationManager getSynchronizationManager() {
        return synchronizationManager;
    }

    public void setUserAccount(UserAccount userAccount) {
        if (userAccount != null) {
            if (this.userAccount == null || this.userAccount.getId() != userAccount.getId()) {
                this.userAccount = userAccount;
                userAccount.setLocalAccountId(userAccountDbHelper.getUserAccountId(userAccount.getAccountName()));
                saveLastUsedAccountInPref();
            }
        } else {
            this.userAccount = null;
        }
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

    public SharedPreferences getUserPreferences() {
        return  getUserPreferences(null);
    }

    public SharedPreferences getUserPreferences(String userName) {
        if (userName == null) {
            if (accountsManager.getUserAccount() != null) {
                userName = accountsManager.getUserAccount().getAccountName();
            } else if (getLastUsedAccount() != null) {
                userName = getLastUsedAccount().getAccountName();
            }
            if (userName != null) {
                SharedPreferences preferences = getSharedPreferences(userName, 0);
                return preferences;
            } else {
                return null;
            }
        }
        return null;
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
            if (serviceClass.getName().equals(service.service.getClassName()) && service.pid != 0) {
                return true;
            }
        }
        return false;
    }

    public void saveParamsInUserPrefs(String name, String value) {
        JSONObject params = new JSONObject();
        try {
            params.put(name, value);
            saveParamsInUserPrefs(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveParamsInUserPrefs(JSONObject params) throws JSONException {
        SharedPreferences userPrefs = getUserPreferences();
        if (userPrefs == null) {
            return;
        }
        SharedPreferences.Editor editor = userPrefs.edit();

        Iterator<?> keys = params.keys();
        while(keys.hasNext()) {
            String key = (String)keys.next();
            if (params.get(key) instanceof String) {
                editor.putString(key, params.get(key).toString());
            }
        }
        editor.commit();
    }

    public void removeParamFromUserPref(String key) {
        SharedPreferences userPrefs = getUserPreferences();
        if (userPrefs == null) {
            return;
        }
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.remove(key);
        editor.commit();
    }

    public JSONObject getParamsFromUserPrefs(String[] requesedParams) throws JSONException {
        JSONObject retrievedParams = new JSONObject();

        SharedPreferences userPreferences = getUserPreferences();
        if (userPreferences != null) {
            String paramValue;
            for (String requesedParam : requesedParams) {
                paramValue = userPreferences.getString(requesedParam, null);
                retrievedParams.put(requesedParam, paramValue);
            }
            return retrievedParams;
        } else {
            return null;
        }
    }

    public void close() {
        if (locationProvider != null) {
            locationProvider.stopUpdates();
        }
        locationProvider = null;
        activationManager.detachAllObservers();
        accountsManager.detachAllObservers();
        accountsManager.prepareToCloseApp();
        synchronizationManager.detachAllObservers();
        tasksManager.prepareToCloseApp();
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