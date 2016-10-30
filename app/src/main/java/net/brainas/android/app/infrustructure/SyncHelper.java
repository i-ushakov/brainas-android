package net.brainas.android.app.infrustructure;

import android.util.Log;
import android.support.v4.util.Pair;

import net.brainas.android.app.CLog;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.synchronization.SynchronizationManager;
import net.brainas.android.app.services.SynchronizationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
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
    static String TAG = "SYNCHRONIZATION";
    private static final String syncDateDir = "/app_sync/sync_data";
    public static final String syncDateDirForSend = syncDateDir + "/for_send/";

    static String lineEnd = "\r\n";
    static String boundary =  "*****";

    private TasksManager tasksManager;
    private TaskChangesDbHelper taskChangesDbHelper;

    public SyncHelper (TasksManager tasksManager,
                       TaskChangesDbHelper taskChangesDbHelper) {
        this.tasksManager = tasksManager;
        this.taskChangesDbHelper = taskChangesDbHelper;
    }

    public static String sendAuthRequest(String accessCode) {
        String response = null;

        HttpsURLConnection connection = null;

        try {
            connection = InfrustructureHelper.createHttpMultipartConn(SynchronizationManager.serverUrl + "authenticate-user");

            DataOutputStream request = new DataOutputStream(
                    connection.getOutputStream());

            request.writeBytes("--" + boundary + lineEnd);

            request.writeBytes("Content-Disposition: form-data; name=\"accessCode\"" + lineEnd);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            request.writeBytes("Content-Length: " + SynchronizationService.accessCode + lineEnd);
            request.writeBytes(lineEnd);
            Log.i(TAG, "Sending code to server " + accessCode);
            request.writeBytes(accessCode);
            request.writeBytes(lineEnd);
            request.writeBytes("--" + boundary + lineEnd);

        } catch (IOException e) {
            e.printStackTrace();
            // TODO No Token situation
            return null;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            // TODO No Token situation
            return null;
        } catch (CertificateException e) {
            e.printStackTrace();
            // TODO No Token situation
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // TODO No Token situation
            return null;
        } catch (KeyManagementException e) {
            e.printStackTrace();
            // TODO No Token situation
            return null;
        }

        // parse server response
        try {
            if (((HttpURLConnection)connection).getResponseCode() == 200) {
                InputStream stream = ((HttpURLConnection) connection).getInputStream();
                InputStreamReader isReader = new InputStreamReader(stream);
                BufferedReader br = new BufferedReader(isReader);
                String line;
                response = "";
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    response += line;
                }
            } else {
                Log.e(TAG, "The Code was sent, but Token haven't gotten! (!= 200)");
                // TODO No Token situation
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "The Code was sent, but Token haven't gotten! (IOException)");
            // TODO No Token situation
            return null;
        }

        if (response.equals("null")) {
            return null;
        }
        return response;
    }

    public static String sendSyncRequest(File allChangesInXML)  {
        String response = "";
        HttpsURLConnection connection = null;
        try {
            connection = InfrustructureHelper.createHttpMultipartConn(SynchronizationManager.serverUrl + "get-tasks");
            connection.setRequestProperty( "Accept-Encoding", "" );
            System.setProperty("http.keepAlive", "false");

            DataOutputStream request = new DataOutputStream(
                    connection.getOutputStream());

            request.writeBytes("--" + boundary + lineEnd);

            if (SynchronizationService.lastSyncTime != null) {
                // set initSync param
                request.writeBytes("Content-Disposition: form-data; name=\"lastSyncTime\"" + lineEnd);
                request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                request.writeBytes("Content-Length: " + SynchronizationService.lastSyncTime.length() + lineEnd);
                request.writeBytes(lineEnd);
                request.writeBytes(SynchronizationService.lastSyncTime);
                request.writeBytes(lineEnd);
                request.writeBytes("--" + boundary + lineEnd);
            }

            // set user identity token
            if (SynchronizationService.accessToken != null) {
                request.writeBytes("Content-Disposition: form-data; name=\"accessToken\"" + lineEnd);
                request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                request.writeBytes("Content-Length: " + SynchronizationService.accessToken.length() + lineEnd);
                request.writeBytes(lineEnd);
                Log.i(TAG, "Sending token " + SynchronizationService.accessToken);
                request.writeBytes(SynchronizationService.accessToken);
                request.writeBytes(lineEnd);
                request.writeBytes("--" + boundary + lineEnd);
            } else {
                Log.i(TAG, "NO ACCESS_TOKEN!!!"); // TODO Stop service and try to restart with new/may be old google ACCESS CODE
                return null;
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
            e.printStackTrace();
            CLog.e(TAG, "Sending sync data to server has failed", e);
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
            Log.e(TAG, "Probably we have a problem with internet connection");
            return null;
        }

        // parse server response
        try {
            if (((HttpsURLConnection)connection).getResponseCode() == 200) {
                InputStream stream = ((HttpURLConnection) connection).getInputStream();
                InputStreamReader isReader = new InputStreamReader(stream);
                BufferedReader br = new BufferedReader(isReader);
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    response += line;
                }
            } else {
                Log.e(TAG, "XML file was sent but error on server is occured");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Getting response sync data from server has failed");
            return null;
        }
        return response;
    }

    public String getAllChangesInXML(HashMap<Long, Pair<String,String>> tasksChanges)
            throws IOException, JSONException, ParserConfigurationException, TransformerException {

        String allChangesInXML;

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

        //Element foldersDriveIdsEl = doc.createElement("foldersIds");
        //foldersDriveIdsEl.setTextContent(GoogleDriveManager.getInstance(BrainasApp.getAppContext()).getFoldersIds().toString());
        //root.appendChild(foldersDriveIdsEl);

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
}
