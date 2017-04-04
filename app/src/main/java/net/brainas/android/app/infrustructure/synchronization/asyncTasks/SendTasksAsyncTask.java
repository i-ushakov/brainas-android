package net.brainas.android.app.infrustructure.synchronization.asyncTasks;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import net.brainas.android.app.CLog;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.SyncHelper;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.infrustructure.synchronization.HandleServerResponseTask;
import net.brainas.android.app.infrustructure.synchronization.SynchronizationManager;
import net.brainas.android.app.services.SynchronizationService;

import java.io.File;

/**
 * Created by Kit Ushakov on 5/9/2016.
 */
public class SendTasksAsyncTask extends AsyncTask<File, Void, String> {
    public static String RESPONSE_STATUS_INVALID_TOKEN = "INVALID_TOKEN";

    protected static String TAG = "SendTasksAsyncTask";

    private AllTasksSyncListener mListener = null;
    private Exception mError = null;
    private File allChangesInXMLFile;
    private UserAccount userAccount;
    private TasksManager tasksManager;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private Integer accountId;
    private SynchronizationService service;


    public static SendTasksAsyncTask build(
            final File allChangesInXMLFile,
            UserAccount userAccount,
            TasksManager tasksManager,
            TaskDbHelper taskDbHelper,
            TaskChangesDbHelper taskChangesDbHelper,
            Integer accountId,
            SynchronizationService service) {

        final SendTasksAsyncTask sendTasksAsyncTask = new SendTasksAsyncTask(
                allChangesInXMLFile,
                userAccount,
                tasksManager,
                taskDbHelper,
                taskChangesDbHelper,
                accountId,
                service);

        sendTasksAsyncTask.setListener(new SendTasksAsyncTask.AllTasksSyncListener() {
            @Override
            public void onComplete(String response, Exception e) {
                sendTasksAsyncTask.handleResponse(response);
                sendTasksAsyncTask.deleteChangesXML(allChangesInXMLFile);
            }});

        return sendTasksAsyncTask;
    }

    public static void deleteChangesXML(File allChangesInXMLFile) {
        if (allChangesInXMLFile != null) {
            if (allChangesInXMLFile.exists()) {
                allChangesInXMLFile.delete();
            }
        }
    }

    protected SendTasksAsyncTask(
            File allChangesInXMLFile,
            UserAccount userAccount,
            TasksManager tasksManager,
            TaskDbHelper taskDbHelper,
            TaskChangesDbHelper taskChangesDbHelper,
            Integer accountId,
            SynchronizationService service) {

        this.allChangesInXMLFile = allChangesInXMLFile;
        this.userAccount = userAccount;
        this.tasksManager = tasksManager;
        this.taskDbHelper = taskDbHelper;
        this.taskChangesDbHelper = taskChangesDbHelper;
        this.accountId = accountId;
        this.service = service;
    };

    @Override
    protected String doInBackground(File... files) {
        String response = null;
        // send changes to server for processing
        if (NetworkHelper.isNetworkActive()) {
            response = SyncHelper.sendTasksRequest(allChangesInXMLFile);
        } else {
            Log.i(TAG, "Network is not available");
        }

        return response;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (this.mListener != null)
            this.mListener.onComplete(s, mError);
    }

    @Override
    protected void onCancelled() {
        if (this.mListener != null) {
            mError = new InterruptedException("AsyncTask cancelled");
            this.mListener.onComplete(null, mError);
        }
    }

    public SendTasksAsyncTask setListener(AllTasksSyncListener listener) {
        this.mListener = listener;
        return this;
    }

    public interface AllTasksSyncListener {
        public void onComplete(String jsonString, Exception e);
    }

    private void handleResponse(String response) {
        if (response != null && response.equals(RESPONSE_STATUS_INVALID_TOKEN)) {
            CLog.e(TAG, "We have error on server: invalid access token", null);
            Crashlytics.log(Log.ERROR, TAG, "We have error on server: invalid access token");
            //notifyAboutServiceMustBeStopped(false, ERR_TYPE_INVALID_TOKEN);
            return;
        }

        HandleServerResponseTask handleServerResponseTask = new HandleServerResponseTask();
        handleServerResponseTask.setUserAccount(userAccount);
        handleServerResponseTask.setTaskManager(tasksManager);
        handleServerResponseTask.setTaskDbHelper(taskDbHelper);
        handleServerResponseTask.setTaskChangesDbHelper(taskChangesDbHelper);
        handleServerResponseTask.setListener(new HandleServerResponseTask.HandleServerResponseListener() {
            @Override
            public void onComplete(String jsonString, Exception e) {
                // remove sending status from changes log after sync is completed
                taskChangesDbHelper.removeAllSendingStatus(accountId); //TODO possibale logic error
                // notify about updates
                // TODO Move this notivication and service object to GetTasksRequestTask
                service.notifyAboutSyncronization();
            }
        });
        handleServerResponseTask.execute(response);
    }
}
