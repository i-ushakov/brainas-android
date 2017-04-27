package net.brainas.android.app.infrustructure.synchronization;

import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Image;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.googleDriveApi.GoogleDriveManager;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.services.SynchronizationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by innok on 7/17/2016.
 */
public class HandleServerResponseTask extends AsyncTask<String, Void, Void> {
    private static String SYNC_TAG = "SYNCHRONIZATION";

    private HandleServerResponseListener mListener = null;

    private UserAccount userAccount;
    private TasksManager tasksManager;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;

    public interface HandleServerResponseListener {
        void onComplete(String jsonString, Exception e);
    }

    public HandleServerResponseTask setListener(HandleServerResponseListener listener) {
        this.mListener = listener;
        return this;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public void setTaskManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    public void setTaskDbHelper(TaskDbHelper taskDbHelper) {
        this.taskDbHelper = taskDbHelper;
    }

    public void setTaskChangesDbHelper(TaskChangesDbHelper taskChangesDbHelper) {
        this.taskChangesDbHelper = taskChangesDbHelper;
    }

    @Override
    protected Void doInBackground(String... params) {

        JSONObject syncDate = null;
        JSONObject synchronizedObjects = null;
        String response = params[0];

        // parse response from server
        if (response != null) {
            Log.i(SYNC_TAG, response);
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(response));
                Document xmlDocument = builder.parse(is);
                syncDate = parseResponse(xmlDocument);
            } catch (ParserConfigurationException | IOException | SAXException |JSONException e) {
                Log.e(SYNC_TAG, "Cannot parse xml-document that gotten from server");
                e.printStackTrace();
                return null;
            }
            if (syncDate == null) {
                return null;
            }

            try {
                synchronizedObjects = (JSONObject)syncDate.get("synchronizedObjects");
                //SynchronizationService.lastSyncTime = (String) syncDate.get("lastSyncTime");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // uncheck or remove synchronized tasks from sync List in local database, set globalId
            try {
                ArrayList<Pair<Long, Long>> synchronizedTasks = (ArrayList<Pair<Long, Long>>)synchronizedObjects.get("synchronizedTasks");
                for (Pair<Long, Long> synchronizedTask : synchronizedTasks) {
                    Long localTaskId = synchronizedTask.first;
                    Long globalTaskId = synchronizedTask.second;
                    if (localTaskId != 0) {
                        Task task = tasksManager.getTaskByLocalId(localTaskId);
                        // task can be null if user deleted it while synchronisation
                        if (task != null) {
                            task.setGlobalId(globalTaskId);
                            tasksManager.saveTask(task, false, false);
                            taskChangesDbHelper.setFlagAfterSuccessfullSync(localTaskId);
                        } else {
                            taskChangesDbHelper.removeFromSync(globalTaskId);
                        }
                    } else {
                        taskChangesDbHelper.removeFromSync(globalTaskId);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // set globalId for synchronized conditions
            try {
                ArrayList<Pair<Long, Long>> synchronizedConditions = (ArrayList<Pair<Long, Long>>)synchronizedObjects.get("synchronizedConditions");
                for (Pair<Long, Long>synchronizedCondition : synchronizedConditions) {
                    Long localConditionId = synchronizedCondition.first;
                    Long globalConditionId = synchronizedCondition.second;
                    if (localConditionId != 0 && globalConditionId != 0) {
                        taskDbHelper.setConditionGlobalId(localConditionId, globalConditionId);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // set globalId for synchronized events
            try {
                ArrayList<Pair<Long, Long>> synchronizedEvents = (ArrayList<Pair<Long, Long>>)synchronizedObjects.get("synchronizedEvents");
                for (Pair<Long, Long>synchronizedEvent : synchronizedEvents) {
                    Long localEventId = synchronizedEvent.first;
                    Long globalEventId = synchronizedEvent.second;
                    if (localEventId != 0 && globalEventId != 0) {
                        taskDbHelper.setEventGlobalId(localEventId, globalEventId);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        mListener.onComplete(null, null);

    }

    @Override
    protected void onCancelled() {
       Log.e(SYNC_TAG, "HandleServerResponseTask was cancelled");
    }

    private  JSONObject parseResponse(Document responseXmlDocument) throws ParserConfigurationException, IOException, SAXException, JSONException {
        JSONObject syncDate = new JSONObject();
        List<Task> updatedTasks;
        ArrayList<Integer> deletedTasks;

        syncDate.put("synchronizedObjects", retriveSynchronizedObjects(responseXmlDocument));

        return syncDate;
    }

    /**
     * This function parsing and handling synchronizer section
     * in xml Document that was got from server. It uncheck objects from sync list
     * and save global ids
     *
     * @param xmlDocument - xml got from server
     * @return synchronizedObjects - JSON with arrays of pairs local and global ids
     */
    public JSONObject retriveSynchronizedObjects(Document xmlDocument) {
        JSONObject synchronizedObjects = new JSONObject();
        ArrayList<Pair<Long,Long>> synchronizedTasks = new ArrayList<>();
        Element synchronizedTasksEl = (Element)xmlDocument.getElementsByTagName("synchronizedTasks").item(0);
        // Synchronized Tasks
        NodeList synchronizedTasksNL = synchronizedTasksEl.getElementsByTagName("synchronizedTask");
        if (synchronizedTasksNL != null) {
            for (int i = 0; i < synchronizedTasksNL.getLength(); ++i) {
                Element synchronizedTaskEl = (Element) synchronizedTasksNL.item(i);
                long localTaskId = Long.valueOf(synchronizedTaskEl.getElementsByTagName("localId").item(0).getTextContent());
                long globalTaskId = Long.valueOf(synchronizedTaskEl.getElementsByTagName("globalId").item(0).getTextContent());
                synchronizedTasks.add(new Pair<>(localTaskId, globalTaskId));
            }
        }

        // Synchronized Conditions
        ArrayList<Pair<Long,Long>> synchronizedConditions = new ArrayList<Pair<Long,Long>>();
        NodeList synchronizedConditionsND = synchronizedTasksEl.getElementsByTagName("synchronizedCondition");
        if (synchronizedConditionsND != null) {
            for (int i = 0; i < synchronizedConditionsND.getLength(); ++i) {
                Element synchronizedConditionEl = (Element) synchronizedConditionsND.item(i);
                long localConditionId = Long.valueOf(synchronizedConditionEl.getElementsByTagName("localId").item(0).getTextContent());
                long globalConditionId = Long.valueOf(synchronizedConditionEl.getElementsByTagName("globalId").item(0).getTextContent());
                synchronizedConditions.add(new Pair<>(localConditionId,globalConditionId));

            }
        }

        // Synchronized Events
        ArrayList<Pair<Long,Long>> synchronizedEvents = new ArrayList<Pair<Long,Long>>();
        NodeList synchronizedEventsND = synchronizedTasksEl.getElementsByTagName("synchronizedEvent");
        if (synchronizedEventsND != null) {
            for (int i = 0; i < synchronizedEventsND.getLength(); ++i) {
                Element synchronizedEventEl = (Element) synchronizedEventsND.item(i);
                long localEventId = Long.valueOf(synchronizedEventEl.getElementsByTagName("localId").item(0).getTextContent());
                long globalEventId = Long.valueOf(synchronizedEventEl.getElementsByTagName("globalId").item(0).getTextContent());
                synchronizedEvents.add(new Pair<>(localEventId,globalEventId));
            }
        }

        try {
            synchronizedObjects.put("synchronizedTasks", synchronizedTasks);
            synchronizedObjects.put("synchronizedConditions", synchronizedConditions);
            synchronizedObjects.put("synchronizedEvents", synchronizedEvents);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return synchronizedObjects;
    }
}
