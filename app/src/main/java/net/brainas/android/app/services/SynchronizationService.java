package net.brainas.android.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.AuthAsyncTask;
import net.brainas.android.app.infrustructure.InfrustructureHelper;
import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.SyncHelper;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.TasksSyncAsyncTask;
import net.brainas.android.app.infrustructure.UserAccount;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by innok on 3/31/2016.
 */
public class SynchronizationService extends Service {
    public static final String BROADCAST_ACTION_SYNCHRONIZATION = "net.brainas.android.app.services.synchronization";
    public static final String BROADCAST_ACTION_SYNCHRONIZATION_MUST_BE_STOPPED = "net.brainas.android.app.services.synchronizationMustBeStopped";

    private static String TAG = "SYNCHRONIZATION";
    public static final String SERVICE_NAME = "synchronization";
    public static String RESPONSE_STATUS_INVALID_TOKEN = "INVALID_TOKEN";

    public static String initSyncTime = null;
    public static String accessToken = null;
    public static String accessCode = null;
    public static String accountName = null;
    public static Integer accountId = null;

    public BrainasApp app;
    public static  AccountsManager accountManager;
    public static  UserAccount userAccount;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> syncThreadHandle = null;
    private TasksSyncAsyncTask tasksSyncAsyncTask;

    private SyncHelper syncHelper;
    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private TasksManager tasksManager;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        syncHelper = new SyncHelper(tasksManager, taskChangesDbHelper, taskDbHelper, userAccount);

        if (accessCode != null && NetworkHelper.isNetworkActive()) {
            fetchTokenAndStartSync();
        } else if (accessToken != null) {
            startSynchronization();
        } else {
            notifyAboutServiceMustBeStopped();
        }

        return Service.START_STICKY;
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
            public void onComplete(String accessToken, Exception e) {
                if (accessToken != null) {
                    SynchronizationService.accessToken = accessToken;
                    userAccount.setAccessToken(accessToken);
                    AccountsManager.saveUserAccount(userAccount);
                    startSynchronization();
                    Log.i(TAG, "We have gotten accessToken = " + accessToken + " from server by accessCode");
                } else {
                    notifyAboutServiceMustBeStopped();
                    Log.i(TAG, "We still not have accessToken; synchronization was stopped");
                    return;
                }
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
                scheduler.scheduleAtFixedRate(syncTask, 5, 50, java.util.concurrent.TimeUnit.SECONDS);

        Log.i(TAG, "Syncronization service was started for user with account id = " + accountId +
                " with access code = " + accessCode +
                " and accessToken =" + accessToken);
    }

    @Override
    public void onDestroy() {
        stopSynchronization();
        Log.i(TAG, "Syncronization service was destroyed");
    }



    public void stopSynchronization() {
        if (syncThreadHandle != null) {
            syncThreadHandle.cancel(true);
        }
        initSyncTime = null;
        accessToken = null;
        accessCode = null;
    }

    public void synchronization() {
        String allChangesInXML;
        File allChangesInXMLFile = null;

        // Retrieve all changes from database and prepare for sending in the form of XML-file
        try {
            allChangesInXML = syncHelper.getAllChangesInXML(accountId);
            allChangesInXMLFile = InfrustructureHelper.createFileInDir(SyncHelper.syncDateDirForSend, "all_changes", "xml");
            final File allChangesInXMLFileFinal = allChangesInXMLFile;
            Files.write(allChangesInXML, allChangesInXMLFile, Charsets.UTF_8);
            Log.i(TAG, allChangesInXMLFile.getName() + " was created");
            Log.i(TAG, Utils.printFileToString(allChangesInXMLFile));

            tasksSyncAsyncTask = new TasksSyncAsyncTask();
            tasksSyncAsyncTask.setListener(new TasksSyncAsyncTask.AllTasksSyncListener() {
                @Override
                public void onComplete(String response, Exception e) {
                    handleResponseFromServer(response);
                    deleteChangesXML(allChangesInXMLFileFinal);
                }});
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
                tasksSyncAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, allChangesInXMLFile);
            else {
                tasksSyncAsyncTask.execute(allChangesInXMLFile);
            }
        } catch (IOException | JSONException | ParserConfigurationException | TransformerException e) {
            Log.e(TAG, "Cannot create XML-file with changes");
            deleteChangesXML(allChangesInXMLFile);
            e.printStackTrace();
            return;
        }
    }

    private void deleteChangesXML(File allChangesInXMLFile) {
        if (allChangesInXMLFile != null) {
            if (allChangesInXMLFile.exists()) {
                allChangesInXMLFile.delete();
            }
        }
    }
    private void handleResponseFromServer (String response) {
        if (response != null && response.equals(RESPONSE_STATUS_INVALID_TOKEN)) {
            notifyAboutServiceMustBeStopped();
            return;
        }

        syncHelper.handleResponseFromServer(response);

        // notify about updates
        notifyAboutSyncronization();
    }

    private void notifyAboutSyncronization() {
        Intent  intent = new Intent(BROADCAST_ACTION_SYNCHRONIZATION);
        //intent.putExtra("activatedTasksIds", TextUtils.join(", ", activatedTasksIds));
        Log.i(TAG, "Notify About Syncronization");
        sendBroadcast(intent);
    }

    private void notifyAboutServiceMustBeStopped() {
        userAccount.setAccessCode(null);
        userAccount.setAccessToken(null);
        accountManager.saveUserAccount(userAccount);
        Intent  intent = new Intent(BROADCAST_ACTION_SYNCHRONIZATION_MUST_BE_STOPPED);
        sendBroadcast(intent);
        Log.i(TAG, "Something went wrong, for example we couldn't exchange code on token, so service must be stopped");
    }
}
