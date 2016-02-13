package net.brainas.android.app.infrustructure;

import android.util.Log;
import android.util.Pair;


import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private static final String syncDateDirForSend = syncDateDir + "/for_send/";


    TasksManager tasksManager;
    TaskChangesDbHelper taskChangesDbHelper;

    SyncHelper(TaskChangesDbHelper taskChangesDbHelper, TasksManager tasksManager) {
        this.tasksManager = tasksManager;
        this.taskChangesDbHelper = taskChangesDbHelper;
    }

    public File getLastChangedTasksInXml() {
        return new File("test");
    }

    public File getAllChangesInXML() throws IOException, JSONException, ParserConfigurationException, TransformerException {
        File changesFile = InfrustructureHelper.createFileInDir(syncDateDirForSend, "all_changes", "xml");
        HashMap<Long, Pair<String,String>> tasksChanges = taskChangesDbHelper.getChangedTasks();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        org.w3c.dom.Element root = (Element) doc.createElement("changes");
        doc.appendChild(root);

        Element changedTasksEl = (Element) doc.createElement("changedTasks");
        root.appendChild(changedTasksEl);

        Iterator it = tasksChanges.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                long taskId = (long) pair.getKey();
                Pair<String, String> change = (Pair<String, String>) pair.getValue();
                Task task = tasksManager.getTaskByLocalId(taskId);
                Element changedTaskEl;
                if (task != null) {
                    changedTaskEl = InfrustructureHelper.taskToXML(doc, task, "changedTask");
                } else {
                    changedTaskEl = doc.createElement("changedTask");
                    Long globalId = taskChangesDbHelper.getGlobalIdOfDeletedTask(taskId);
                    changedTaskEl.setAttribute("globalId", globalId.toString());
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
        Files.write(writer.toString(), changesFile, Charsets.UTF_8);
        return changesFile;
    }

    public void handleResultOfSyncWithServer(Element synchronizedObjects) {
        NodeList synchronizedTask = synchronizedObjects.getElementsByTagName("synchronizedTask");
        if (synchronizedTask != null) {
            for (int i = 0; i < synchronizedTask.getLength(); ++i) {
                Element synchronizedTaskEl = (Element) synchronizedTask.item(i);
                long localTaskId = Long.valueOf(synchronizedTaskEl.getElementsByTagName("localId").item(0).getTextContent());
                long globalTaskId = Long.valueOf(synchronizedTaskEl.getElementsByTagName("globalId").item(0).getTextContent());
                if (localTaskId != 0) {
                    Task task = tasksManager.getTaskByLocalId(localTaskId);
                    task.setGlobalId(globalTaskId);
                    task.save(false, false);
                    taskChangesDbHelper.uncheckFromSync(localTaskId);
                } else {
                    taskChangesDbHelper.removeFromSync(globalTaskId);
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
        Task task = tasksManager.getTaskByLocalId(globalId);
        if (task != null) {
            String timeOfLocalChanges = taskChangesDbHelper.getTimeOfLastChanges(task.getId());
            if (Utils.compareTwoDates(timeOfServerChanges, timeOfLocalChanges) > 0) {
                return true;
            }
        } else {
            return true;
        }

        return false;
    }
}
