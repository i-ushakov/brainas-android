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
        ArrayList<Integer> deletedTasksFromServer = null;
        ArrayList<Task> updatedTasksFromServer = null;
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
                deletedTasksFromServer = (ArrayList<Integer>)syncDate.get("deletedTasks");
                updatedTasksFromServer = (ArrayList<Task>)syncDate.get("updatedTasks");
                SynchronizationService.lastSyncTime = (String) syncDate.get("lastSyncTime");
                if ((String) syncDate.get("accessToken") != null) {
                    SynchronizationService.accessToken = (String) syncDate.get("accessToken");
                    userAccount.setAccessToken(SynchronizationService.accessToken);
                    AccountsManager.saveUserAccount(userAccount);
                    Log.v(SYNC_TAG, "Access token was gotten :" + SynchronizationService.accessToken);
                }
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
                            taskChangesDbHelper.uncheckFromSync(localTaskId);
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

            // delete tasks in DB (that previously were deleted on server)
            for(Integer deletedTaskId : deletedTasksFromServer) {
                tasksManager.deleteTaskByGlobalId(deletedTaskId);
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

        syncDate.put("lastSyncTime", retrieveTimeOfLastSync(responseXmlDocument));
        syncDate.put("accessToken", retrieveAccessToken(responseXmlDocument));

        updatedTasks = retrieveAndSaveTasksFromServer(responseXmlDocument);
        syncDate.put("updatedTasks", updatedTasks);

        deletedTasks = retrieveDeletedTasksFromServer(responseXmlDocument, "deletedTask");
        syncDate.put("deletedTasks", deletedTasks);

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
        Element synchronizedObjectsEl = (Element)xmlDocument.getElementsByTagName("synchronizedObjects").item(0);
        // Synchronized Tasks
        NodeList synchronizedTasksNL = synchronizedObjectsEl.getElementsByTagName("synchronizedTask");
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
        NodeList synchronizedConditionsND = synchronizedObjectsEl.getElementsByTagName("synchronizedCondition");
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
        NodeList synchronizedEventsND = synchronizedObjectsEl.getElementsByTagName("synchronizedEvent");
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

    /**
     * Retrieve TimeOfInitialSync from server's xml response
     *
     * @param xmlDocument - xml got from server
     * @return accessToken
     */
    public String retrieveTimeOfLastSync(Document xmlDocument) {
        String lastSyncTime;
        Element lastSyncTimeEl= (Element)xmlDocument.getElementsByTagName("lastSyncTime").item(0);
        if (lastSyncTimeEl != null) {
            lastSyncTime = lastSyncTimeEl.getTextContent();
            return lastSyncTime;
        } else {
            Log.v(SYNC_TAG, "We have a problem, we can't get a lastSyncTime from server");
            return null;
        }
    }

    /**
     * Retrieving access token from xml response from server
     *
     * @param xmlDocument - xml-document that was got from server
     * @return accessToken
     */
    public String retrieveAccessToken(Document xmlDocument) {
        Log.i("TOKEN_TEST", "retrieveAccessToken mothod");
        String accessToken;

        Element accessTokenEl = (Element)xmlDocument.getElementsByTagName("accessToken").item(0);
        if (accessTokenEl != null) {
            accessToken = accessTokenEl.getTextContent();
            if (!accessToken.equals("")) {
                Log.i("TOKEN_TEST", "return accessToken = " + accessToken);
                return accessToken;
            } else {
                return null;
            }
        } else {
            Log.v(SYNC_TAG, "We have a problem, we can't get a accessToken from server");
            return null;
        }
    }

    /**
     * Retrieving an updated and created tasks from xml response from server
     *
     * @param xmlDocument - xml-document that was got from server
     * @return updatedTasks
     */
    public ArrayList<Task> retrieveAndSaveTasksFromServer(Document xmlDocument) {
        ArrayList<Task> updatedTasks = new ArrayList<>();
        NodeList taskList = xmlDocument.getElementsByTagName("task");

        for (int i = 0; i < taskList.getLength(); ++i) {
            Element taskEl = (Element)taskList.item(i);
            int globalId = Integer.parseInt(taskEl.getAttribute("global-id"));
            String timeOfServerChanges = taskEl.getAttribute("time-changes");
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
        return updatedTasks;
    }

    /**
     * Parsing the xml-document that was got from server
     * and retrieving list with global ids of deleted tasks.
     * We check the relevance of server changes (deleting) before add to final list
     *
     * @param xmlDocument - xml-document that was got from server
     * @param tagName - name of tag for deleted tasks in xml
     * @return deletedTasks
     * @throws JSONException
     */
    public ArrayList<Integer> retrieveDeletedTasksFromServer(Document xmlDocument, String tagName) throws JSONException {
        ArrayList<Integer> deletedTasks = new ArrayList<Integer>();
        NodeList deletedTasksList = xmlDocument.getElementsByTagName(tagName);
        for (int i = 0; i < deletedTasksList.getLength(); ++i) {
            Element deletedTaskEl = (Element)deletedTasksList.item(i);
            int globalId = Integer.parseInt(deletedTaskEl.getAttribute("global-id"));
            //String timeChanges = deletedTaskEl.getAttribute("time-changes");
            //if(checkTheRelevanceOfTheChanges(globalId, timeChanges)){
            deletedTasks.add(globalId);
            //}
        }
        return deletedTasks;
    }

    /**
     * Comare dattime of server's changes with datetime of local last changes
     *
     * @param globalId
     * @param timeOfServerChanges
     * @return accessToken
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
}
