package net.brainas.android.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.util.Pair;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.BrainasAppSettings;
import net.brainas.android.app.CLog;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.infrustructure.AuthAsyncTask;
import net.brainas.android.app.infrustructure.synchronization.asyncTasks.RefreshTokenAsyncTask;
import net.brainas.android.app.infrustructure.synchronization.asyncTasks.SendTasksAsyncTask;
import net.brainas.android.app.infrustructure.googleDriveApi.GoogleDriveManager;
import net.brainas.android.app.infrustructure.InfrustructureHelper;
import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.SyncHelper;
import net.brainas.android.app.infrustructure.SyncSettingsWithServerTask;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import io.fabric.sdk.android.Fabric;

/**
 * Created by innok on 3/31/2016.
 */
public class SynchronizationService extends Service {
    public static final String BROADCAST_ACTION_SYNCHRONIZATION = "net.brainas.android.app.services.synchronization";
    public static final String BROADCAST_ACTION_SYNCHRONIZATION_MUST_BE_STOPPED = "net.brainas.android.app.services.synchronizationMustBeStopped";
    public static final String ERR_TYPE_CANNOT_EXCHANGE_CODE_ON_TOKEN = "CANNOT_EXCHANGE_CODE_ON_TOKEN";
    public static final String ERR_TYPE_NO_INTERNET_FOR_EXCHANGE_CODE = "NO_INTERNET_FOR_EXCHANGE_CODE";
    public static final String ERR_TYPE_NO_ACCESS_CODE = "NO_ACCESS_CODE";
    public static final String ERR_TYPE_INVALID_TOKEN = "INVALID_TOKEN";

    private static String TAG = "#SYNC_SERVICE";
    public static final String SERVICE_NAME = "synchronization";

    public static String lastSyncTime = null;
    public static String accessToken = null;
    public static String accessCode = null;
    public static String accountName = null;
    public static Integer accountId = null;

    public BrainasApp app;
    public static UserAccount userAccount;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> syncThreadHandle = null;
    private RefreshTokenAsyncTask refreshTokenAsyncTask;
    private SendTasksAsyncTask tasksSyncAsyncTask;

    private SyncHelper syncHelper;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private TasksManager tasksManager;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Fabric.with(getApplicationContext(), new Crashlytics());
        app = ((BrainasApp) BrainasApp.getAppContext());
        Crashlytics.log(Log.ERROR, TAG, "onStartCommand");
        initialiseSyncService(intent);
        return Service.START_STICKY;
    }

    @Override
    // this solution from http://stackoverflow.com/questions/3072173/how-to-call-a-method-after-a-delay-in-android
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "Sync Service: onTaskRemoved");
        /*Intent restartIntent = new Intent(app, ServiceMustBeAliveReceiver.class);
        restartIntent.putExtra("serviceClass", "SynchronizationService");
        restartIntent.putExtra("accountName",accountName);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(app, 2002, restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis() + 3000, pendingIntent);
        } else {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis() + 3000, pendingIntent);
        }*/

        Intent restartIntent = new Intent();
        restartIntent.putExtra("serviceClass", "SynchronizationService");
        restartIntent.putExtra("accountName",accountName);
        restartIntent.setAction(ServiceMustBeAliveReceiver.SYNC_INTENT);
        app.sendBroadcast(restartIntent);
    }

    private void initialiseSyncService(Intent intent) {
        initHelpers();
        if (intent != null) {
            accountName = intent.getExtras().getString("accountName");
            userAccount = AccountsManager.getUserAccountByName(accountName);
            setUserSyncParams(userAccount);
            Log.i(TAG, "Sync service started with intent; accessCode = " + accessCode + "; accessToken = " + accessToken);
        } else {
            userAccount = app.getLastUsedAccount();
            setUserSyncParams(userAccount);
            Log.i(TAG, "Sync service started in background; accessCode = " + accessCode + "; accessToken = " + accessToken);
        }

        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, userAccount.getId());
        syncHelper = new SyncHelper(tasksManager, taskChangesDbHelper);

        if (accessCode != null && NetworkHelper.isNetworkActive()) {
            fetchTokenAndStartSync();
        } else if (accessToken != null) {
            startSynchronization();
        } else {
            if (accessCode == null ) {
                Log.i(TAG, "No accesss CODE for get TOKEN");
                notifyAboutServiceMustBeStopped(false, ERR_TYPE_NO_ACCESS_CODE);
            } else if (accessCode != null && !NetworkHelper.isNetworkActive()) {
                Log.i(TAG, "No internet to exchange CODE on TOKEN");
                notifyAboutServiceMustBeStopped(true, ERR_TYPE_NO_INTERNET_FOR_EXCHANGE_CODE);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initHelpers() {
        app = ((BrainasApp)BrainasApp.getAppContext());
        taskDbHelper = app.getTaskDbHelper();
        taskChangesDbHelper = app.getTasksChangesDbHelper();
    }

    private void setUserSyncParams(UserAccount userAccount) {
        accountId = userAccount.getId();
        accessCode = userAccount.getAccessCode();
        accessToken = userAccount.getAccessToken();
    }

    private void fetchTokenAndStartSync() {
        AuthAsyncTask authAsyncTask = new AuthAsyncTask();
        authAsyncTask.setListener(new AuthAsyncTask.AuthAsyncTaskListener() {
            @Override
            public void onComplete(String result, Exception e) {
                if (result != null) {
                    String accessToken = result;
                    SynchronizationService.accessToken = accessToken;
                    userAccount.setAccessToken(accessToken);
                    AccountsManager.saveUserAccount(userAccount);
                    startSynchronization();
                    Log.i(TAG, "We have gotten accessToken = " + accessToken + " from server by accessCode");
                    return;
                }

                CLog.e(TAG, "We still not have accessToken. Cannot exchange access CODE on TOKEN", null);
                notifyAboutServiceMustBeStopped(false, ERR_TYPE_CANNOT_EXCHANGE_CODE_ON_TOKEN);
                return;
            }
        });

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
            authAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, accessCode);
        else {
            authAsyncTask.execute(accessCode);
        }
        userAccount.setAccessCode(null);
        AccountsManager.saveUserAccount(userAccount);
    }

    private void startSynchronization() {
        new SyncSettingsWithServerTask(accessToken, new SyncSettingsWithServerTask.Callback() {
            @Override
            public void onSyncSuccess() {
                GoogleDriveManager.getInstance(app).manageAppFolders();
            }

            @Override
            public void onSyncFailed() {

            }
        }).execute();
        final Handler handler = new Handler();
        TimerTask syncTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if(NetworkHelper.isNetworkActive()) {
                            Log.i(TAG, "Sync was start!");
                            synchronization();
                            Log.i(TAG, "Sync was done!");
                        } else {
                            Log.i(TAG, "NetworkActive wis not active!");
                        }
                    }
                });
            }
        };

        syncThreadHandle =
                scheduler.scheduleAtFixedRate(syncTask,
                        BrainasAppSettings.getSynchronizationSartTime(),
                        BrainasAppSettings.getSynchronizationInterval(),
                        java.util.concurrent.TimeUnit.SECONDS);

        Log.i(TAG, "Syncronization service was started for user with account id = " + accountId +
                " with access code = " + accessCode +
                " and accessToken =" + accessToken);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSynchronization();
        //app.getSynchronizationManager().removeServiceAlarm();
        Log.i(TAG, "Syncronization service was destroyed");
    }



    public void stopSynchronization() {
        if (syncThreadHandle != null) {
            syncThreadHandle.cancel(true);
        }
        lastSyncTime = null;
        accessToken = null;
        accessCode = null;
        notifyAboutServiceMustBeStopped(false, null);
    }

    public void synchronization() {
        String allChangesInXML;
        File allChangesInXMLFile = null;

        // Retrieve all changes from database and prepare for sending in the form of XML-file
        try {
            HashMap<Long, Pair<String,String>> tasksChanges = taskChangesDbHelper.getChangesOfTasks(accountId);
            allChangesInXML = syncHelper.getAllChangesInXML(tasksChanges);
            allChangesInXMLFile = InfrustructureHelper.createFileInDir(InfrustructureHelper.getPathToSendDir(accountId), "all_changes", "xml");
            final File allChangesInXMLFileFinal = allChangesInXMLFile;
            Files.write(allChangesInXML, allChangesInXMLFile, Charsets.UTF_8);
            Log.i(TAG, allChangesInXMLFile.getName() + " was created");
            Log.i(TAG, Utils.printFileToString(allChangesInXMLFile));

            refreshTokenAsyncTask = RefreshTokenAsyncTask.build(userAccount);
            refreshTokenAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            tasksSyncAsyncTask = SendTasksAsyncTask.build(
                    allChangesInXMLFile,
                    userAccount,
                    tasksManager,
                    taskDbHelper,
                    taskChangesDbHelper,
                    accountId,
                    this);
            tasksSyncAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            // we check in changes log, that we are sending these changes to server now
            taskChangesDbHelper.setStatusToSending(tasksChanges.keySet().toArray(new Long[tasksChanges.keySet().size()]));
        } catch (IOException | JSONException | ParserConfigurationException | TransformerException e) {
            Log.e(TAG, "Cannot create XML-file with changes");
            SendTasksAsyncTask.deleteChangesXML(allChangesInXMLFile);
            e.printStackTrace();
            return;
        }
    }

    public void notifyAboutSyncronization() {
        Intent intent = new Intent(BROADCAST_ACTION_SYNCHRONIZATION);
        //intent.putExtra("activatedTasksIds", TextUtils.join(", ", activatedTasksIds));
        Log.i(TAG, "Notify About Syncronization");
        sendBroadcast(intent);
    }

    private void notifyAboutServiceMustBeStopped(boolean restartSync, String errType) {
        Intent  intent = new Intent(BROADCAST_ACTION_SYNCHRONIZATION_MUST_BE_STOPPED);
        if (restartSync == true) {
            Log.i(TAG, "Something went wrong, may be internet connection problem, so sync service must be restart");
            intent.putExtra("restartSync", true);
            intent.putExtra("accountName", userAccount.getAccountName());
        } else {
            Log.i(TAG, "Something went wrong, for example we couldn't get access token, so sync service must be stopped");
            intent.putExtra("restartSync", false);
            intent.putExtra("errType", errType);
            userAccount.setAccessCode(null);
            userAccount.setAccessToken(null);
            AccountsManager.saveUserAccount(userAccount);
        }
        sendBroadcast(intent);
    }
}
