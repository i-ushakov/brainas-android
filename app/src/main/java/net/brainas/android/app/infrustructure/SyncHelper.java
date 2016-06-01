package net.brainas.android.app.infrustructure;

import android.util.Log;
import android.support.v4.util.Pair;

import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.services.SynchronizationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
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

    private TasksManager tasksManager;
    private TaskChangesDbHelper taskChangesDbHelper;
    private TaskDbHelper taskDbHelper;
    private ServicesDbHelper servicesDbHelper;
    private int accountId;

    public SyncHelper (TasksManager tasksManager,
                       TaskChangesDbHelper taskChangesDbHelper,
                       TaskDbHelper taskDbHelper,
                       ServicesDbHelper servicesDbHelper,
                       int accountId) {
        this.tasksManager = tasksManager;
        this.taskChangesDbHelper = taskChangesDbHelper;
        this.taskDbHelper = taskDbHelper;
        this.servicesDbHelper = servicesDbHelper;
        this.accountId =  accountId;
    }

    public static String sendAllChanges(File allChangesInXML)  {
        String response = "";
        HttpsURLConnection connection = null;
        try {
            connection = InfrustructureHelper.createHttpMultipartConn(SynchronizationManager.serverUrl + "get-tasks");

            DataOutputStream request = new DataOutputStream(
                    connection.getOutputStream());

            request.writeBytes("--" + boundary + lineEnd);

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
                Log.i("TOKEN_TEST", "Sending token " + SynchronizationService.accessToken);
                request.writeBytes(SynchronizationService.accessToken);
                request.writeBytes(lineEnd);
                request.writeBytes("--" + boundary + lineEnd);
            } else {
                request.writeBytes("Content-Disposition: form-data; name=\"accessCode\"" + lineEnd);
                request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                request.writeBytes("Content-Length: " + SynchronizationService.accessCode + lineEnd);
                request.writeBytes(lineEnd);
                Log.i("TOKEN_TEST", "Sending code " + SynchronizationService.accessCode);
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
        } catch (IOException e) {
            Log.e(TAG, "Sending sync data to server has failed");
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        } catch (KeyManagementException e) {
            e.printStackTrace();
            return null;
        } catch (ClassCastException e) {
            Log.i(TAG, "Probably we have a problem with internet connection");
            return null;
        }

        // parse server response
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Getting response sync data from server has failed");
        }

        return response;
    }

    /**
     *  Getting all changes that still isn't synchronized with the server in format of XML-String
     *
     * @param accountId - user account id
     * @return allChangesInXML - string with all changes that must be synchronized with
     */
    public String getAllChangesInXML(int accountId)
            throws IOException, JSONException, ParserConfigurationException, TransformerException {

        String allChangesInXML;

        HashMap<Long, android.support.v4.util.Pair<String,String>> tasksChanges = taskChangesDbHelper.getChangedTasks(accountId);

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("changes");
        doc.appendChild(root);

        Element existingTasks = doc.createElement("existingTasks");
        existingTasks.setTextContent(getAllExistingTasksWithGlobalId().toString());
        root.appendChild(existingTasks);

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
                    changedTaskEl = TaskHelper.taskToXML(doc, task, "changedTask");
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

    public JSONObject getAllExistingTasksWithGlobalId() {
        JSONObject existingTasks = new JSONObject();
        ArrayList<Task> tasks = tasksManager.getAllTasks();
        for (Task task : tasks) {
            if (task.getGlobalId() != 0) {
                try {
                    existingTasks.put(Long.toString(task.getGlobalId()), Long.toString(task.getId()));
                } catch (JSONException e) {
                    Log.e(TAG, "Cannot create JSONObject with existing tasks that have globalId");
                    e.printStackTrace();
                }
            }
        }
        return existingTasks;
    }

    /**
     *  Handle response from server
     *
     * @param response - xml string with response from server
     */
    public void handleResponseFromServer(String response) {
        JSONObject syncDate = null;
        JSONObject synchronizedObjects = null;
        ArrayList<Integer> deletedTasksFromServer = null;
        ArrayList<Task> updatedTasksFromServer = null;

        // parse response from server
        if (response != null) {
            Log.i(TAG, response);
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(response));
                Document xmlDocument = builder.parse(is);
                syncDate = parseResponse(xmlDocument);
            } catch (ParserConfigurationException | IOException | SAXException |JSONException e) {
                Log.e(TAG, "Cannot parse xml-document that gotten from server");
                e.printStackTrace();
                return;
            }
            if (syncDate == null) {
                return;
            }

            try {
                synchronizedObjects = (JSONObject)syncDate.get("synchronizedObjects");
                deletedTasksFromServer = (ArrayList<Integer>)syncDate.get("deletedTasks");
                updatedTasksFromServer = (ArrayList<Task>)syncDate.get("updatedTasks");
                SynchronizationService.initSyncTime = (String) syncDate.get("initSyncTime");
                if ((String) syncDate.get("accessToken") != null) {
                    SynchronizationService.accessToken = (String) syncDate.get("accessToken");
                    Log.i("TOKEN_TEST", "Saving accessToken " + SynchronizationService.accessToken + " for service " + SynchronizationService.SERVICE_NAME);
                    servicesDbHelper.addServiceParam(SynchronizationService.SERVICE_NAME, "accessToken", SynchronizationService.accessToken);
                    Log.v(TAG, "Access token was gotten :" + SynchronizationService.accessToken);
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
            return;
        }
        return;
    }

    private  JSONObject parseResponse(Document responseXmlDocument) throws ParserConfigurationException, IOException, SAXException, JSONException {
        JSONObject syncDate = new JSONObject();
        List<Task> updatedTasks;
        ArrayList<Integer> deletedTasks;

        syncDate.put("synchronizedObjects", retriveSynchronizedObjects(responseXmlDocument));

        syncDate.put("initSyncTime", retrieveTimeOfInitialSync(responseXmlDocument));
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
            Log.v(TAG, "We have a problem, we can't get a accessToken from server");
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
                task = new Task(this.accountId, message);
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
                Integer conditionGlobalId = Integer.parseInt(conditionEl.getAttribute("id"));
                Condition condition = new Condition(null,conditionGlobalId, task.getId());
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
            String timeChanges = deletedTaskEl.getAttribute("time-changes");
            if(checkTheRelevanceOfTheChanges(globalId, timeChanges)){
                deletedTasks.add(globalId);
            }
        }
        return deletedTasks;
    }
}
