package net.brainas.android.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.InfrustructureHelper;
import net.brainas.android.app.infrustructure.ServicesDbHelper;
import net.brainas.android.app.infrustructure.SyncHelper;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.AllTasksSync;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    private static String TAG = "SynchronizationService";
    public static final String SERVICE_NAME = "synchronization";

    public static String initSyncTime = null;
    public static String accessToken = null;
    public static String accessCode = null;
    public static Integer accountId;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> syncThreadHandle = null;
    private AllTasksSync asyncTask;

    private SyncHelper syncHelper;
    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private ServicesDbHelper servicesDbHelper;
    private TasksManager tasksManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        appDbHelper = new AppDbHelper(this);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        taskChangesDbHelper = new TaskChangesDbHelper(appDbHelper);
        servicesDbHelper = new ServicesDbHelper(appDbHelper);
        if (intent != null) {
            accountId = intent.getExtras().getInt("accountId");
            accessCode = intent.getExtras().getString("accessCode");
            JSONObject serviceParamsJSON = new JSONObject();
            try {
                Log.i("TOKEN_TEST", "Sync serv start with intent");
                serviceParamsJSON.put("accountId", accountId);
                serviceParamsJSON.put("accessCode", accessCode);
                servicesDbHelper.saveServiceParams(SERVICE_NAME, serviceParamsJSON.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Log.i("TOKEN_TEST", "Sync serv start background");
                JSONObject serviceParamsJSON = new JSONObject(servicesDbHelper.getServiceParams(SERVICE_NAME));
                accountId = serviceParamsJSON.getInt("accountId");
                accessCode = serviceParamsJSON.getString("accessCode");
                accessToken = serviceParamsJSON.getString("accessToken");
                Log.i("TOKEN_TEST", "Tokent from db" + accessToken);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, accountId);
        syncHelper = new SyncHelper(tasksManager, taskChangesDbHelper, taskDbHelper, servicesDbHelper, accountId);
        startSynchronization();
        Log.i(TAG, "Syncronization service was started for user with account id = " + accountId +
                " with access code = " + accessCode +
                " and accessToken =" + accessToken);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startSynchronization() {
        final Handler handler = new Handler();
        TimerTask syncTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        Log.i(TAG, "Sync was start!");
                        synchronization();
                        Log.i(TAG, "Sync was done!");
                    }
                });
            }
        };

        syncThreadHandle =
                scheduler.scheduleAtFixedRate(syncTask, 5, 50, java.util.concurrent.TimeUnit.SECONDS);
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
        File allChangesInXMLFile;
        String allChangesInXML;

        // Retrieve all changes from database and prepare for sending in the form of XML-file
        try {
            allChangesInXML = syncHelper.getAllChangesInXML(accountId);
            allChangesInXMLFile = InfrustructureHelper.createFileInDir(SyncHelper.syncDateDirForSend, "all_changes", "xml");
            Files.write(allChangesInXML, allChangesInXMLFile, Charsets.UTF_8);
            Log.v(TAG, allChangesInXMLFile.getName() + " was created");
            Log.v(TAG, Utils.printFileToString(allChangesInXMLFile));
        } catch (IOException | JSONException | ParserConfigurationException | TransformerException e) {
            Log.e(TAG, "Cannot create XML-file with changes");
            e.printStackTrace();
            return;
        }

        asyncTask = new AllTasksSync();
        asyncTask.setListener(new AllTasksSync.AllTasksSyncListener() {
            @Override
            public void onComplete(String response, Exception e) {
                handleResponseFromServer(response);
            }});
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, allChangesInXMLFile);
        else {
            asyncTask.execute(allChangesInXMLFile);
        }

        // TODO Remove File
    }

    private void handleResponseFromServer (String response) {

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
}
