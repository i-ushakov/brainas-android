package net.brainas.android.app.infrustructure;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by Kit Ushakov on 11/15/2015.
 *
 *This class is responsible for synchronization of data
 * on mobile device and web server. It's periodically doing synchronization
 * in background, sends all changes to server and accepts changes from server side.
 */

public class Synchronization {
    private static Synchronization instance = null;

    //static String serverUrl = "http://192.168.1.104/backend/web/connection/";
    static String serverUrl = "http://brainas.net/backend/web/connection/";

    static String TAG = "SYNC";

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> syncThreadHandle = null;
    private AllTasksSync asyncTask;

    public BrainasApp app;
    public SyncHelper syncHelper;
    public TasksManager tasksManager;
    public TaskChangesDbHelper tasksChangesDbHelper;

    public static String initSyncTime = null;
    public static String accessToken = null;
    public static String accessCode = null;

    private List<TaskSyncObserver> observers = new ArrayList<>();

    public interface TaskSyncObserver {
        void updateAfterSync();
    }

    public void attach(TaskSyncObserver observer){
        observers.add(observer);
    }

    public void detach(TaskSyncObserver observer){
        observers.remove(observer);
    }

    private Synchronization() {
        app = ((BrainasApp)((BrainasApp.getAppContext())));
        tasksChangesDbHelper = app.getTasksChangesDbHelper();
        tasksManager = app.getTasksManager();
        syncHelper = new SyncHelper();
    }

    public static synchronized Synchronization getInstance() {
        if(instance == null) {
            instance = new Synchronization();
        }
        return instance;
    }

    public void startSynchronization(UserAccount userAccount) {
        accessCode = userAccount.getAccessCode();
        final Handler handler = new Handler();
        TimerTask syncTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (((BrainasApp)BrainasApp.getAppContext()).getAccountsManager().isUserAuthorized()) {
                            Log.v("SYNC", "Sync was start!");
                            synchronization();
                            Log.v("SYNC", "Sync was done!");
                        }
                    }
                });
            }
        };

        syncThreadHandle =
                scheduler.scheduleAtFixedRate(syncTask, 5, 35, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stopSynchronization() {
        if (syncThreadHandle != null) {
            syncThreadHandle.cancel(true);
        }
        Synchronization.initSyncTime = null;
        Synchronization.accessToken = null;
        Synchronization.accessCode = null;
    }

    private void synchronization() {
        asyncTask = new AllTasksSync();
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else {
            asyncTask.execute();
        }
    }

    private class AllTasksSync extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... files) {
            File allChangesInXMLFile;
            String allChangesInXML;
            String response = null;
            List<Task> updatedTasksFromServer;
            ArrayList<Integer> deletedTasksFromServer;

            // Retrieve all changes from database and prepare for sending in the form of XML-file
            try {
                allChangesInXML = syncHelper.getAllChangesInXML();
                allChangesInXMLFile = InfrustructureHelper.createFileInDir(SyncHelper.syncDateDirForSend, "all_changes", "xml");
                Files.write(allChangesInXML, allChangesInXMLFile, Charsets.UTF_8);
                Log.v(TAG, allChangesInXMLFile.getName() + " was created");
                Log.v(TAG, Utils.printFileToString(allChangesInXMLFile));
            } catch (IOException | JSONException | ParserConfigurationException | TransformerException e) {
                Log.e(TAG, "Cannot create XML-file with changes");
                e.printStackTrace();
                return null;
            }

            // send changes to server for processing
            try {
                response = SyncHelper.sendAllChanges(allChangesInXMLFile);
            } catch (IOException e) {
                Log.e(TAG, "Exchange of sync data has failed");
                e.printStackTrace();
            }


            // parse response from server
            if (response != null) {
                try {
                    JSONObject syncDate = parseResponse(response);
                    updatedTasksFromServer = (ArrayList<Task>)syncDate.get("tasks");
                    deletedTasksFromServer = (ArrayList<Integer>)syncDate.get("deletedTasks");
                } catch (ParserConfigurationException | IOException | SAXException |JSONException e) {
                    Log.e(TAG, "Cannot parse xml-document that gotten from server");
                    e.printStackTrace();
                    return null;
                }

                // refreshe and delete tasks in DB
                syncHelper.updateTasksInDb(updatedTasksFromServer);
                syncHelper.deleteTasksFromDb(deletedTasksFromServer);
            }

            // notify about updates
            notifyAllObservers();

            return null;
        }
    }

    private  JSONObject parseResponse(String xmlStr) throws ParserConfigurationException, IOException, SAXException, JSONException {
        JSONObject syncDate = new JSONObject();
        List<Task> tasks;
        ArrayList<Integer> deletedTasks;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlStr));
        Document xmlDocument = builder.parse(is);

        syncHelper.handleResultOfSyncWithServer(xmlDocument);
        initSyncTime = syncHelper.retrieveTimeOfInitialSync(xmlDocument);
        String accessToken = syncHelper.retrieveAccessToken(xmlDocument);
        if (accessToken != null) {
            Synchronization.accessToken = accessToken;
            Log.v(TAG, "Access token was gotten :" + Synchronization.accessToken);
        }

        tasks = syncHelper.retriveCreatedAndUpdatetTasks(xmlDocument);
        syncDate.put("tasks", tasks);

        deletedTasks = syncHelper.retriveDeletedTasks(xmlDocument, "deletedTask");
        syncDate.put("deletedTasks", deletedTasks);

        return syncDate;
    }

    private void notifyAllObservers() {
        for (TaskSyncObserver observer : observers) {
            observer.updateAfterSync();
        }
    }
}
