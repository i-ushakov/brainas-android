package net.brainas.android.app.infrustructure;

import android.util.Log;
import android.support.v4.util.Pair;


import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.services.SynchronizationService;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by innok on 2/5/2016.
 */
public class SyncHelper {
    static String TAG = "SYNC";
    private static final String syncDateDir = "/app_sync/sync_data";
    public static final String syncDateDirForSend = syncDateDir + "/for_send/";

    static String lineEnd = "\r\n";
    static String boundary =  "*****";
    static String xmlOutDirPath = "/app_sync/xml/out";

    private TasksManager tasksManager;
    private TaskChangesDbHelper taskChangesDbHelper;
    private TaskDbHelper taskDbHelper;

    public SyncHelper (TasksManager tasksManager, TaskChangesDbHelper taskChangesDbHelper, TaskDbHelper taskDbHelper) {
        this.tasksManager = tasksManager;
        this.taskChangesDbHelper = taskChangesDbHelper;
        this.taskDbHelper = taskDbHelper;
    }


    /*
     * Getting all changes that still isn't synchronized with the server in format of XML-String
     * <?xml version="1.0" encoding="UTF-8"?>
     * <changedTasks>
     *     <changedTask globalId="1" id="1">
     *         <message>message</message>
     *          <change>
     *              <status>UPDATED</status>
     *              <changeDatetime>2016-02-12 10:14:41</changeDatetime>
     *          </change>
     *     </changedTask>
     * </changedTasks>
     */
    public String getAllChangesInXML(int accountId)
            throws IOException, JSONException, ParserConfigurationException, TransformerException {

        String allChangesInXML;

        HashMap<Long, android.support.v4.util.Pair<String,String>> tasksChanges = taskChangesDbHelper.getChangedTasks(accountId);

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("changes");
        doc.appendChild(root);
        Element changedTasksEl = doc.createElement("changedTasks");
        root.appendChild(changedTasksEl);

        Iterator it = tasksChanges.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                long localId = (long) pair.getKey();
                Pair<String, String> change = (Pair<String, String>) pair.getValue();

                Task task = tasksManager.getTaskByLocalId(localId);
                Element changedTaskEl;
                if (task != null) {
                    changedTaskEl = InfrustructureHelper.taskToXML(doc, task, "changedTask");
                } else {
                    Long globalId = taskChangesDbHelper.getGlobalIdOfDeletedTask(localId);
                    if (globalId != 0 && change.first.equals("DELETED")) {
                        changedTaskEl = doc.createElement("changedTask");
                        changedTaskEl.setAttribute("globalId", globalId.toString());
                        changedTaskEl.setAttribute("id", Long.toString(localId));
                    } else {
                        // We don't need send info about the deleted task, that is not known for server
                        taskChangesDbHelper.uncheckFromSync(localId);
                        continue;
                    }
                }
                Element changeEl = doc.createElement("change");
                Element statusEl = doc.createElement("status");
                statusEl.setTextContent(change.first);
                changeEl.appendChild(statusEl);
                Element datetimeEl = doc.createElement("changeDatetime");
                datetimeEl.setTextContent(change.second);
                changeEl.appendChild(datetimeEl);
                changedTaskEl.appendChild(changeEl);
                changedTasksEl.appendChild(changedTaskEl);
            }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(new DOMSource(doc), result);
        allChangesInXML = writer.toString();

        return allChangesInXML;
    }

    public static String sendAllChanges(File allChangesInXML) throws IOException {
        String response = "";
        HttpURLConnection connection = InfrustructureHelper.createHttpMultipartConn(SynchronizationManager.serverUrl + "get-tasks");

        DataOutputStream request = new DataOutputStream(
                connection.getOutputStream());

        request.writeBytes("--" + boundary + lineEnd);

        /*if (initSync) {
            // set initSync param
            String initSync = "true";
            request.writeBytes("Content-Disposition: form-data; name=\"initSync\"" + lineEnd);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            request.writeBytes("Content-Length: " + initSync.length() + lineEnd);
            request.writeBytes(lineEnd);
            request.writeBytes(initSync);
            request.writeBytes(lineEnd);
            request.writeBytes("--" + boundary + lineEnd);
        }*/

        if (SynchronizationService.initSyncTime != null) {
            // set initSync param
            String initSync = "true";
            request.writeBytes("Content-Disposition: form-data; name=\"initSyncTime\"" + lineEnd);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            request.writeBytes("Content-Length: " + SynchronizationService.initSyncTime.length() + lineEnd);
            request.writeBytes(lineEnd);
            request.writeBytes(SynchronizationService.initSyncTime);
            request.writeBytes(lineEnd);
            request.writeBytes("--" + boundary + lineEnd);
        }

        // set user identity token
        if (SynchronizationService.accessToken != null) {
            request.writeBytes("Content-Disposition: form-data; name=\"accessToken\"" + lineEnd);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            request.writeBytes("Content-Length: " + SynchronizationService.accessToken.length() + lineEnd);
            request.writeBytes(lineEnd);
            request.writeBytes(SynchronizationService.accessToken);
            request.writeBytes(lineEnd);
            request.writeBytes("--" + boundary + lineEnd);
        } else {
            request.writeBytes("Content-Disposition: form-data; name=\"accessCode\"" + lineEnd);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            request.writeBytes("Content-Length: " + SynchronizationService.accessCode + lineEnd);
            request.writeBytes(lineEnd);
            request.writeBytes(SynchronizationService.accessCode);
            request.writeBytes(lineEnd);
            request.writeBytes("--" + boundary + lineEnd);
        }


        // attach file with all changes
        request.writeBytes("Content-Disposition: form-data; " +
                "name=\"" + "all_changes_xml" + "\"" +
                "; filename=\"" + allChangesInXML.getName() + "\"" + lineEnd);
        request.writeBytes("Content-Type: text/xml" + lineEnd);
        request.writeBytes("Content-Length: " + allChangesInXML.length() + lineEnd);
        request.writeBytes(lineEnd);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(allChangesInXML));
        byte[] buffer = new byte[(int) allChangesInXML.length()];
        bis.read(buffer);
        request.write(buffer);
        request.writeBytes(lineEnd);
        request.writeBytes("--" + boundary + lineEnd);
        bis.close();

        // parse server response
        if (((HttpURLConnection)connection).getResponseCode() == 200) {
            InputStream stream = ((HttpURLConnection) connection).getInputStream();
            InputStreamReader isReader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(isReader);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                response += line + System.getProperty("line.separator");
            }
        } else {
            Log.e(TAG, "XML file was sent but error on server is occured");
            return null;
        }

        return response;

    }

    public void handleResultOfSyncWithServer(Document xmlDocument) {
        Element synchronizedObjectsEl = (Element)xmlDocument.getElementsByTagName("synchronizedObjects").item(0);
        // Synchronized Tasks
        NodeList synchronizedTasks = synchronizedObjectsEl.getElementsByTagName("synchronizedTask");
        if (synchronizedTasks != null) {
            for (int i = 0; i < synchronizedTasks.getLength(); ++i) {
                Element synchronizedTaskEl = (Element) synchronizedTasks.item(i);
                long localTaskId = Long.valueOf(synchronizedTaskEl.getElementsByTagName("localId").item(0).getTextContent());
                long globalTaskId = Long.valueOf(synchronizedTaskEl.getElementsByTagName("globalId").item(0).getTextContent());
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
        }

        // Synchronized Conditions
        NodeList synchronizedConditions = synchronizedObjectsEl.getElementsByTagName("synchronizedCondition");
        if (synchronizedConditions != null) {
            for (int i = 0; i < synchronizedConditions.getLength(); ++i) {
                Element synchronizedConditionEl = (Element) synchronizedConditions.item(i);
                long localConditionId = Long.valueOf(synchronizedConditionEl.getElementsByTagName("localId").item(0).getTextContent());
                long globalConditionId = Long.valueOf(synchronizedConditionEl.getElementsByTagName("globalId").item(0).getTextContent());
                if (localConditionId != 0 && globalConditionId != 0) {
                    taskDbHelper.setConditionGlobalId(localConditionId, globalConditionId);
                }
            }
        }

        // Synchronized Events
        NodeList synchronizedEvents = synchronizedObjectsEl.getElementsByTagName("synchronizedEvent");
        if (synchronizedEvents != null) {
            for (int i = 0; i < synchronizedEvents.getLength(); ++i) {
                Element synchronizedEventEl = (Element) synchronizedEvents.item(i);
                long localConditionId = Long.valueOf(synchronizedEventEl.getElementsByTagName("localId").item(0).getTextContent());
                long globalConditionId = Long.valueOf(synchronizedEventEl.getElementsByTagName("globalId").item(0).getTextContent());
                if (localConditionId != 0 && globalConditionId != 0) {
                    taskDbHelper.setEventGlobalId(localConditionId, globalConditionId);
                }
            }
        }
    }

    public String retrieveTimeOfInitialSync(Document xmlDocument) {
        String initSyncTime;
        Element initSyncTimeEl = (Element)xmlDocument.getElementsByTagName("initSyncTime").item(0);
        if (initSyncTimeEl != null) {
            initSyncTime = initSyncTimeEl.getTextContent();
            return initSyncTime;
        } else {
            Log.v(TAG, "We have a problem, we can't get a initSyncTime from server");
            return null;
        }
    }

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

    public String retrieveAccessToken(Document xmlDocument) {
        String accessToken;

        Element accessTokenEl = (Element)xmlDocument.getElementsByTagName("accessToken").item(0);
        if (accessTokenEl != null) {
            accessToken = accessTokenEl.getTextContent();
            if (!accessToken.equals("")) {
                return accessToken;
            } else {
                return null;
            }
        } else {
            Log.v(TAG, "We have a problem, we can't get a accessToken from server");
            return null;
        }
    }

    public void handlingOfTasksFromServer(Document xmlDocument, int accountId) {
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
                task = new Task(accountId, message);
                task.setGlobalId(globalId);
                tasksManager.saveTask(task, false, false);
            } else {
                task.setMessage(message);
            }
            task.setDescription(description);
            Element statusEl = (Element)taskEl.getElementsByTagName("status").item(0);
            if (statusEl != null) {
                task.setStatus(statusEl.getTextContent());
            }

            // Conditions
            CopyOnWriteArrayList<Condition> conditions = new CopyOnWriteArrayList<Condition>();
            NodeList conditionsNL = taskEl.getElementsByTagName("condition");
            for (int j = 0; j < conditionsNL.getLength(); ++j) {
                Element conditionEl = (Element)conditionsNL.item(j);
                Integer conditionId = Integer.parseInt(conditionEl.getAttribute("id"));
                Condition condition = new Condition(null,conditionId, task.getId());
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
        }
    }

    public ArrayList<Integer> retriveDeletedTasks(Document xmlDocument, String type) throws JSONException {
        ArrayList<Integer> deletedTasks = new ArrayList<Integer>();
        NodeList deletedTasksList = xmlDocument.getElementsByTagName(type);
        for (int i = 0; i < deletedTasksList.getLength(); ++i) {
            Element deletedTaskEl = (Element)deletedTasksList.item(i);
            int globalId = Integer.parseInt(deletedTaskEl.getAttribute("global-id"));
            String timeChanges = deletedTaskEl.getAttribute("time-changes");
            if(checkTheRelevanceOfTheChanges(globalId, timeChanges)){
                deletedTasks.add(globalId);
            }
        }
        return deletedTasks;
    }

    public void deleteTasksFromDb(ArrayList<Integer> deletedTasks) {
        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TasksManager tasksManager = app.getTasksManager();
        for(Integer deletedTaskId : deletedTasks) {
            tasksManager.deleteTaskByGlobalId(deletedTaskId);
        }
    }
}
