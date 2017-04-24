package net.brainas.android.app.infrustructure.synchronization.asyncTasks;

import android.os.AsyncTask;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.CLog;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Image;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.InfrustructureHelper;
import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.SyncHelper;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.infrustructure.googleDriveApi.GoogleDriveManager;
import net.brainas.android.app.infrustructure.synchronization.HandleServerResponseTask;
import net.brainas.android.app.services.SynchronizationService;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by Kit Ushakov on 5/9/2016.
 */
public class GetTasksAsyncTask extends AsyncTask<File, Void, String> {
    public static String RESPONSE_STATUS_INVALID_TOKEN = "INVALID_TOKEN";

    protected static String TAG = "SendTasksAsyncTask";

    private AllTasksSyncListener mListener = null;
    private Exception mError = null;
    private UserAccount userAccount;
    private TasksManager tasksManager;
    private TaskChangesDbHelper taskChangesDbHelper;
    private Integer accountId;


    public static GetTasksAsyncTask build(
            UserAccount userAccount,
            TasksManager tasksManager,
            TaskDbHelper taskDbHelper,
            TaskChangesDbHelper taskChangesDbHelper,
            Integer accountId,
            SynchronizationService service) {

        final GetTasksAsyncTask getTasksAsyncTask = new GetTasksAsyncTask(
                userAccount,
                tasksManager,
                taskDbHelper,
                taskChangesDbHelper,
                accountId,
                service);

        getTasksAsyncTask.setListener(new GetTasksAsyncTask.AllTasksSyncListener() {
            @Override
            public void onComplete(String response, Exception e) {
                getTasksAsyncTask.handleResponse(response);
            }});

        return getTasksAsyncTask;
    }

    protected GetTasksAsyncTask(
            UserAccount userAccount,
            TasksManager tasksManager,
            TaskDbHelper taskDbHelper,
            TaskChangesDbHelper taskChangesDbHelper,
            Integer accountId,
            SynchronizationService service) {

        this.userAccount = userAccount;
        this.tasksManager = tasksManager;
        this.taskChangesDbHelper = taskChangesDbHelper;
        this.accountId = accountId;
    };

    @Override
    protected String doInBackground(File... files) {
        String response = null;
        if (NetworkHelper.isNetworkActive()) {
            File existsTasksInXmlFile = null;
            try {
                SyncHelper syncHelper = new SyncHelper(tasksManager, taskChangesDbHelper);
                String existsTasksInXml = syncHelper.getAllExisitsTasksInXML();
                existsTasksInXmlFile = InfrustructureHelper.createFileInDir(InfrustructureHelper.getPathToSendDir(accountId), "exists_tasks_xml", "xml");
                Files.write(existsTasksInXml, existsTasksInXmlFile, Charsets.UTF_8);
                response = SyncHelper.getTasksRequest(existsTasksInXmlFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
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

    public GetTasksAsyncTask setListener(AllTasksSyncListener listener) {
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

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);

            ArrayList<Task> updatedTasks = new ArrayList<>();
            NodeList taskList = xmlDocument.getElementsByTagName("task");

            for (int i = 0; i < taskList.getLength(); ++i) {
                Element taskEl = (Element)taskList.item(i);
                int globalId = Integer.parseInt(taskEl.getAttribute("globalId"));
                String timeOfServerChanges = taskEl.getAttribute("timeOfChange");
                if (!checkTheRelevanceOfTheChanges(globalId, timeOfServerChanges)) {
                    continue;
                }

                String message = taskEl.getElementsByTagName("message").item(0).getTextContent();
                String description = taskEl.getElementsByTagName("description").item(0).getTextContent();
                Task task = tasksManager.getTaskByGlobalId(globalId);
                if (task == null) {
                    task = new Task(userAccount.getId(), message);
                    task.setGlobalId(globalId);
                    tasksManager.saveTask(task, false, false);
                } else {
                    task.setMessage(message);
                }
                task.setDescription(description);

                // Picture
                Element pictureEl = (Element)taskEl.getElementsByTagName("picture").item(0);
                if (pictureEl != null) {
                    Element pictureNameEl = (Element)pictureEl.getElementsByTagName("name").item(0);
                    if (pictureNameEl != null) {
                        Image currentPicture = task.getPicture();
                        if (currentPicture == null || !currentPicture.getName().equals(pictureNameEl.getTextContent()) || currentPicture.getBitmap() == null) {
                            Image picture = new Image(pictureNameEl.getTextContent());
                            Element pictureResourceIdEl = (Element)pictureEl.getElementsByTagName("resourceId").item(0);
                            if (pictureResourceIdEl != null && pictureResourceIdEl.getTextContent() != null) {
                                picture.setResourceId(pictureResourceIdEl.getTextContent());
                            }
                            if (picture.getDriveId() == null && picture.getResourceId() != null) {
                                picture.setDriveId(GoogleDriveManager.getInstance(BrainasApp.getAppContext()).fetchDriveIdByResourceId(picture.getResourceId(), null));
                            }
                            task.setPicture(picture);
                            //Bitmap pictureBitmap = InfrustructureHelper.getTaskPicture(task.getPicture().getName());
                            //if (pictureBitmap == null) {
                            GoogleDriveManager.getInstance(BrainasApp.getAppContext()).downloadPicture(picture, userAccount.getId());
                            //}
                        }
                    }
                }

                Element statusEl = (Element)taskEl.getElementsByTagName("status").item(0);
                if (statusEl != null) {
                    task.setStatus(statusEl.getTextContent());
                }

                // Conditions
                CopyOnWriteArrayList<Condition> conditions = new CopyOnWriteArrayList<Condition>();
                NodeList conditionsNL = taskEl.getElementsByTagName("condition");
                for (int j = 0; j < conditionsNL.getLength(); ++j) {
                    Element conditionEl = (Element)conditionsNL.item(j);
                    Integer conditionGlobalId = Integer.parseInt(conditionEl.getAttribute("id"));
                    Condition condition = new Condition(null, conditionGlobalId, task.getId());
                    NodeList events = conditionEl.getElementsByTagName("event");
                    Event event = null;
                    Element eventEl = null;
                    for(int k = 0; k < events.getLength(); ++k) {
                        event = null;
                        eventEl = (Element)events.item(k);
                        String type = eventEl.getAttribute("type");
                        int eventId = Integer.parseInt(eventEl.getAttribute("id"));
                        switch (type) {
                            case "GPS" :
                                event = new EventLocation(null, eventId, null);
                                event.fillInParamsFromXML(eventEl);
                                break;

                            case "TIME" :
                                event = new EventTime(null, eventId, null);
                                event.fillInParamsFromXML(eventEl);
                                break;
                        }
                        if (event != null) {
                            condition.addEvent(event);
                        }
                    }
                    conditions.add(condition);
                }
                task.setConditions(conditions);
                tasksManager.saveTask(task);
                updatedTasks.add(task);
            }
            SynchronizationService.lastSyncTime = retrieveTimeOfLastSync(xmlDocument);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Comare dattime of server's changes with datetime of local last changes
     *
     * @param globalId
     * @param timeOfServerChanges
     */
    public boolean checkTheRelevanceOfTheChanges(long globalId, String timeOfServerChanges) {
        Task task = tasksManager.getTaskByGlobalId(globalId);
        if (task != null) {
            String timeOfLocalChanges = taskChangesDbHelper.getTimeOfLastChanges(task.getId());
            if (timeOfLocalChanges != null) {
                if (Utils.compareTwoDates(timeOfServerChanges, timeOfLocalChanges) > 0) {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }

        return false;
    }

    /**
     * Retrieve TimeOfInitialSync from server's xml response
     *
     * @param xmlDocument - xml got from server
     */
    public String retrieveTimeOfLastSync(Document xmlDocument) {
        String lastSyncTime;
        Element lastSyncTimeEl= (Element)xmlDocument.getElementsByTagName("lastSyncTime").item(0);
        if (lastSyncTimeEl != null) {
            lastSyncTime = lastSyncTimeEl.getTextContent();
            return lastSyncTime;
        } else {
            Log.v(TAG, "We have a problem, we can't get a lastSyncTime from server");
            return null;
        }
    }
}
