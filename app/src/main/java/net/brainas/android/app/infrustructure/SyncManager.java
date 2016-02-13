package net.brainas.android.app.infrustructure;

import android.os.AsyncTask;
import android.os.Build;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.EventGPS;
import net.brainas.android.app.domain.models.Task;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import android.os.Handler;
import android.util.Log;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Kit Ushakov on 11/15/2015.
 */

public class SyncManager {
    private static SyncManager instance = null;
    private boolean initSync = true;

    static String attachmentFileName = "fresh_task.xml";
    //static String serverUrl = "http://192.168.1.104/backend/web/connection/";
    static String serverUrl = "http://brainas.net/backend/web/connection/";
    static String lineEnd = "\r\n";
    static String boundary =  "*****";
    static String xmlOutDirPath = "/app_sync/xml/out";

    static String TAG = "SYNC";

    private List<TaskSyncObserver> observers = new ArrayList<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> syncThreadHandle = null;
    private AllTasksSync asyncTask;
    private JSONObject synchronizedChanges = new JSONObject();
    private JSONObject synchronizedTaskChanges = new JSONObject();
    private SyncHelper syncHelper;
    private String initSyncTime =null;


    private SyncManager() {
        TaskChangesDbHelper tasksChangesDbHelper = ((BrainasApp)((BrainasApp.getAppContext()))).getTasksChangesDbHelper();
        TasksManager tasksManager = ((BrainasApp)((BrainasApp.getAppContext()))).getTasksManager();
        this.syncHelper = new SyncHelper(tasksChangesDbHelper, tasksManager);
    }

    public static synchronized SyncManager getInstance() {
        if(instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    public void startSynchronization() {
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
                scheduler.scheduleAtFixedRate(syncTask, 15, 45, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stopSynchronization() {
        syncThreadHandle.cancel(true);
    }

    public interface TaskSyncObserver {
        void updateAfterSync();
    }

    public void attach(TaskSyncObserver observer){
        observers.add(observer);
    }

    public void detach(TaskSyncObserver observer){
        observers.remove(observer);
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
            File xmlFile;
            File allChangesInXML;
            String response = null;
            List<Task> tasks;
            ArrayList<Integer> deletedTasks;

            // For first sync getting AllTasksChangesListInJSON
            if (initSync == true) {
                Log.v(TAG, "This is the first sync iteration");
                try {
                    allChangesInXML = syncHelper.getAllChangesInXML();
                    Log.v(TAG, allChangesInXML.getName() + " was created");
                    Log.v(TAG, Utils.printFileToString(allChangesInXML));
                } catch (IOException | JSONException | ParserConfigurationException | TransformerException e) {
                    Log.e(TAG, "Cannot create json file with all changes");
                    e.printStackTrace();
                    return null;
                }

                try {
                    response = sendAllChanges(allChangesInXML);
                } catch (IOException e) {
                    Log.e(TAG, "Exchange of sync data has failed");
                    e.printStackTrace();
                }
            }

            if (response != null) {
                try {
                    JSONObject syncDate = parseResponse(response);
                    tasks = (ArrayList<Task>)syncDate.get("tasks");
                    deletedTasks = (ArrayList<Integer>)syncDate.get("deletedTasks");
                } catch (ParserConfigurationException | IOException | SAXException |JSONException e) {
                    Log.e(TAG, "Cannot parse xml-document that gotten from server");
                    e.printStackTrace();
                    return null;
                }


                // refreshes tasks in DB
                updateTasksInDb(tasks);

                deleteTasksFromDb(deletedTasks);
            }


            // Prepare xml
            /*try {
                xmlFile = getFreshTasks();
            } catch (TransformerException | ParserConfigurationException | IOException e) {
                // Cannot create xml with local fresh tasks
                e.printStackTrace();
                return null;
            }*/

            // send
            /*try {
                response = sendFreshTasks(xmlFile);
            } catch (IOException e) {
                // Cannot send data to server for synchronization of tasks
                e.printStackTrace();
                return null;
            }*/

            // parse received xml
            /*try {
                JSONObject syncDate = parseResponse(response);
                tasks = (ArrayList<Task>)syncDate.get("tasks");
                deletedTasks = (ArrayList<Integer>)syncDate.get("deletedTasks");
            } catch (IOException | SAXException | ParserConfigurationException e) {
                // Cannot parse xml-document that gotten from server
                e.printStackTrace();
                return null;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }*/



            /*try {
                sendSynchronizedChanges();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            // notify about fresh tasks
            notifyAllObservers();

            return null;
        }
    }

    private File getFreshTasks() throws TransformerException, ParserConfigurationException, IOException {
        File xmlFile = prepareXmlFileForTasks();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("tasks");
        doc.appendChild(rootElement);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        StreamResult result = new StreamResult(xmlFile);

        transformer.transform(source, result);

        return xmlFile;
    }

    private File prepareXmlFileForTasks() throws IOException {
        File xmlFile;

        String dataDir = BrainasApp.getAppContext().getApplicationInfo().dataDir;

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Calendar cal = Calendar.getInstance();
        String xmlFileName = "fresh_tasks_" + dateFormat.format(cal.getTime()) + ".xml";

        xmlFile = new File(dataDir + xmlOutDirPath + "/" +  xmlFileName);
        xmlFile.getParentFile().mkdirs();
        xmlFile.createNewFile();

        return xmlFile;
    }

    private String sendAllChanges(File allChangesInXML) throws IOException {
        String response = "";
        HttpURLConnection connection = InfrustructureHelper.createHttpMultipartConn(serverUrl + "get-tasks");

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

        if (initSyncTime != null) {
            // set initSync param
            String initSync = "true";
            request.writeBytes("Content-Disposition: form-data; name=\"initSyncTime\"" + lineEnd);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            request.writeBytes("Content-Length: " + initSyncTime.length() + lineEnd);
            request.writeBytes(lineEnd);
            request.writeBytes(initSyncTime);
            request.writeBytes(lineEnd);
            request.writeBytes("--" + boundary + lineEnd);
        }

        // set user identity token
        String accessToken = ((BrainasApp)BrainasApp.getAppContext()).getAccountsManager().getAccessToken();
        request.writeBytes("Content-Disposition: form-data; name=\"accessToken\"" + lineEnd);
        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
        request.writeBytes("Content-Length: " + accessToken.length() + lineEnd);
        request.writeBytes(lineEnd);
        request.writeBytes(accessToken);
        request.writeBytes(lineEnd);
        request.writeBytes("--" + boundary + lineEnd);

        // attach file with all changes
        request.writeBytes("Content-Disposition: form-data; " +
                "name=\"" + "all_changes_xml" + "\"" +
                "; filename=\"" + allChangesInXML.getName() + "\"" + lineEnd);
        request.writeBytes("Content-Type: text/xml" + SyncManager.lineEnd);
        request.writeBytes("Content-Length: " + allChangesInXML.length() + SyncManager.lineEnd);
        request.writeBytes(SyncManager.lineEnd);
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
                response += line;
            }
        } else {
            Log.e(TAG, "XML file was sent but error on server is occured");
            return null;
        }

        return response;

    }

    private String sendFreshTasks(File xmlFileForUpload) throws IOException {
        HttpURLConnection connection = InfrustructureHelper.createHttpMultipartConn(serverUrl + "get-tasks");

        DataOutputStream request = new DataOutputStream(
                connection.getOutputStream());


        request.writeBytes("--" + boundary + lineEnd);
        request.writeBytes("Content-Disposition: form-data; " +
                "name=\"" + SyncManager.attachmentFileName + "\"" +
                "; filename=\"" + xmlFileForUpload.getName() + "\"" + lineEnd);

        request.writeBytes("Content-Type: text/xml" + SyncManager.lineEnd);
        request.writeBytes("Content-Length: " + xmlFileForUpload.length() + SyncManager.lineEnd);
        request.writeBytes(SyncManager.lineEnd);

        BufferedInputStream streamOfXmlDocument = new BufferedInputStream(new FileInputStream(xmlFileForUpload));
        byte[] buffer = new byte[(int) xmlFileForUpload.length()];
        streamOfXmlDocument.read(buffer);
        request.write(buffer);
        request.writeBytes(lineEnd);
        request.writeBytes("--" + boundary + lineEnd);
        streamOfXmlDocument.close();

        String accessToken = ((BrainasApp)BrainasApp.getAppContext()).getAccountsManager().getAccessToken();
        request.writeBytes("Content-Disposition: form-data; name=\"accessToken\"" + lineEnd);
        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
        request.writeBytes("Content-Length: " + accessToken.length() + lineEnd);
        request.writeBytes(lineEnd);
        request.writeBytes(accessToken); // mobile_no is String variable
        request.writeBytes(lineEnd);
        request.writeBytes("--" + boundary + lineEnd);

        // parse server response
        InputStream stream = ((HttpURLConnection)connection).getInputStream();
        InputStreamReader isReader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(isReader);
        String response = "";
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            response+= line;
        }

        return response;
    }

    private  JSONObject parseResponse(String xmlStr) throws ParserConfigurationException, IOException, SAXException, JSONException {
        JSONObject syncDate = new JSONObject();
        List<Task> tasks = new ArrayList<Task>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlStr));
        Document xmlDocument = builder.parse(is);

        Element synchronizedObjectsEl = (Element)xmlDocument.getElementsByTagName("synchronizedObjects").item(0);

        syncHelper.handleResultOfSyncWithServer(synchronizedObjectsEl);
        initSyncTime = syncHelper.retrieveTimeOfInitialSync(xmlDocument);


        NodeList taskList = xmlDocument.getElementsByTagName("task");

        for (int i = 0; i < taskList.getLength(); ++i) {
            Element taskEl = (Element)taskList.item(i);
            int globalId = Integer.parseInt(taskEl.getAttribute("global-id"));
            String timeOfServerChanges = taskEl.getAttribute("time-changes");
            if (!syncHelper.checkTheRelevanceOfTheChanges(globalId, timeOfServerChanges)) {
                continue;
            }

            String timeChanges = taskEl.getAttribute("time-changes");
            if (!isActualChanges(globalId, timeChanges)) {
                synchronizedTaskChanges.put(Integer.toString(globalId), "Rejected");
                continue;
            }
            String message = taskEl.getElementsByTagName("message").item(0).getTextContent();
            String description = taskEl.getElementsByTagName("description").item(0).getTextContent();
            int accountId = ((BrainasApp)BrainasApp.getAppContext()).getUserAccount().getAccountId();
            Task task = new Task(accountId, message);
            task.setGlobalId(globalId);
            task.setDescription(description);
            Element statusEl = (Element)taskEl.getElementsByTagName("status").item(0);
            if (statusEl != null) {
                task.setStatus(statusEl.getTextContent());
            }
            NodeList conditions = taskEl.getElementsByTagName("condition");
            for (int j = 0; j < conditions.getLength(); ++j) {
                Element conditionEl = (Element)conditions.item(j);
                Integer conditionId = Integer.parseInt(conditionEl.getAttribute("id"));
                Integer taskId = Integer.parseInt(conditionEl.getAttribute("task-id"));
                Condition condition = new Condition(null,conditionId, null);
                NodeList events = conditionEl.getElementsByTagName("event");
                for(int k = 0; k < events.getLength(); ++k) {
                    EventGPS event = null;
                    Element eventEl = (Element)events.item(k);
                    String type = eventEl.getAttribute("type");
                    int eventId = Integer.parseInt(eventEl.getAttribute("id"));
                    switch (type) {
                        case "GPS" :
                            event = new EventGPS(null, eventId, null);
                            event.fillInParamsFromXML(eventEl);
                            break;
                    }
                    condition.addEvent(event);
                }
                task.addCondition(condition);
            }
            tasks.add(task);
            synchronizedTaskChanges.put(Integer.toString(globalId), "Accepted");
        }
        syncDate.put("tasks", tasks);

        ArrayList<Integer> deletedTasks = parseDeletedXmlBlock(xmlDocument, "deletedTask");
        syncDate.put("deletedTasks", deletedTasks);

        return syncDate;
    }

    private Map<String,List<Long>> updateTasksInDb(List<Task> tasks) {
        List<Long> addedTasksLocalIds = new ArrayList<Long>();
        List<Long> updatedTasksGlobalIds = new ArrayList<Long>();
        Map<String,List<Long>> result = new HashMap<String,List<Long>>();

        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TaskDbHelper taskDbHelper = app.getTaskDbHelper();

        for (int i = 0; i < tasks.size(); ++i) {
            Task task = tasks.get(i);
            long newLocalTaskId = (long)taskDbHelper.addOrUpdateTask(task);
            if(newLocalTaskId > 0) {
                addedTasksLocalIds.add(newLocalTaskId);
            } else {
                updatedTasksGlobalIds.add(task.getGlobalId());
            }
        }

        result.put("added", addedTasksLocalIds);
        result.put("updated", updatedTasksGlobalIds);
        //taskDbHelper.close();
        return result;
    }

    private void deleteTasksFromDb(ArrayList<Integer> deletedTasks) {
        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TasksManager tasksManager = app.getTasksManager();
        for(Integer deletedTaskId : deletedTasks) {
            tasksManager.deleteTaskByGlobalId(deletedTaskId);
        }
    }

    private void notifyAllObservers() {
        for (TaskSyncObserver observer : observers) {
            observer.updateAfterSync();
        }
    }

    private boolean isActualChanges(long taskId, String datetime) {
        return true;
    }

    private ArrayList<Integer> parseDeletedXmlBlock(Document xmlDocument, String type) throws JSONException {
        ArrayList<Integer> deletedTasks = new ArrayList<Integer>();
        NodeList deletedTasksList = xmlDocument.getElementsByTagName(type);
        for (int i = 0; i < deletedTasksList.getLength(); ++i) {
            Element deletedTaskEl = (Element)deletedTasksList.item(i);
            int globalId = Integer.parseInt(deletedTaskEl.getAttribute("global-id"));
            String timeChanges = deletedTaskEl.getAttribute("time-changes");
            if(isActualChanges(globalId, timeChanges)){
                deletedTasks.add(globalId);
                synchronizedTaskChanges.put(Integer.toString(globalId), "Accepted");
            } else {
                synchronizedTaskChanges.put(Integer.toString(globalId), "Rejected");
            }
        }
        return deletedTasks;
    }

    private void sendSynchronizedChanges() throws JSONException, IOException {
        synchronizedChanges.put("tasks", synchronizedTaskChanges);

        String url = serverUrl + "accepted-changes";
        URL urlObj = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);

        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setRequestProperty("Accept-Encoding", "" );
        connection.connect();

        DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
        //wr.write(synchronizedChanges.toString());
        String str = synchronizedChanges.toString();
        byte[] data=str.getBytes("UTF-8");
        printout.write(data);
        printout.flush();
        printout.close();

        InputStream stream = ((HttpURLConnection)connection).getInputStream();
        InputStreamReader isReader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(isReader);
        String response = "";
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            response+= line;
        }

        //return response;
    }
}
