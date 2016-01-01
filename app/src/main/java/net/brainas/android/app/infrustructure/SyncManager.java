package net.brainas.android.app.infrustructure;

import android.os.AsyncTask;
import android.os.Build;

import net.brainas.android.app.BrainasApp;
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
import java.net.MalformedURLException;
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

    static String attachmentFileName = "fresh_task.xml";
    //static String serverUrl = "http://192.168.1.103/backend/web/connection/";
    static String serverUrl = "http://brainas.net/backend/web/connection/";
    static String lineEnd = "\r\n";
    static String boundary =  "*****";
    static String xmlOutDirPath = "/app_sync/xml/out";

    private List<TaskSyncObserver> observers = new ArrayList<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> syncThreadHandle = null;
    private AllTasksSync asyncTask;
    private JSONObject synchronizedChanges = new JSONObject();
    private JSONObject synchronizedTaskChanges = new JSONObject();


    private SyncManager() {}

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
                        Log.v("SYNC", "Sync was start!");
                        synchronization();
                        Log.v("SYNC", "Sync was done!");
                    }
                });
            }
        };

        syncThreadHandle =
                scheduler.scheduleAtFixedRate(syncTask, 15, 15, java.util.concurrent.TimeUnit.SECONDS);
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
            String response;
            List<Task> tasks;
            ArrayList<Integer> deletedTasks;

            // Prepare xml
            try {
                xmlFile = getFreshTasks();
            } catch (TransformerException | ParserConfigurationException | IOException e) {
                // Cannot create xml with local fresh tasks
                e.printStackTrace();
                return null;
            }

            // send
            try {
                response = sendFreshTasks(xmlFile);
            } catch (IOException e) {
                // Cannot send data to server for synchronization of tasks
                e.printStackTrace();
                return null;
            }

            // parse received xml
            try {
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
            }

            // refreshes tasks in DB
            updateTasksInDb(tasks);

            deleteTasksFromDb(deletedTasks);

            try {
                sendSynchronizedChanges();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private String sendFreshTasks(File xmlFileForUpload) throws IOException {
        String url = serverUrl + "get-tasks";
        URL urlObj = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
        connection.setRequestProperty("Accept","*/*");
        connection.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=" + SyncManager.boundary);

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

        NodeList taskList = xmlDocument.getElementsByTagName("task");

        for (int i = 0; i < taskList.getLength(); ++i) {
            Element taskEl = (Element)taskList.item(i);
            int globalId = Integer.parseInt(taskEl.getAttribute("global-id"));
            String timeChanges = taskEl.getAttribute("time-changes");
            if (!isActualChanges(globalId, timeChanges)) {
                synchronizedTaskChanges.put(Integer.toString(globalId), "Rejected");
                continue;
            }
            String message = taskEl.getElementsByTagName("message").item(0).getTextContent();
            String description = taskEl.getElementsByTagName("description").item(0).getTextContent();
            Task task = new Task(message);
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

    private Map<String,List<Integer>> updateTasksInDb(List<Task> tasks) {
        List<Integer> addedTasksLocalIds = new ArrayList<Integer>();
        List<Integer> updatedTasksGlobalIds = new ArrayList<Integer>();
        Map<String,List<Integer>> result = new HashMap<String,List<Integer>>();

        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TaskDbHelper taskDbHelper = app.getTaskDbHelper();

        for (int i = 0; i < tasks.size(); ++i) {
            Task task = tasks.get(i);
            int newLocalTaskId = (int)taskDbHelper.addOrUpdateTask(task);
            if(newLocalTaskId > 0) {
                addedTasksLocalIds.add(newLocalTaskId);
            } else {
                updatedTasksGlobalIds.add(task.getGlobalId());
            }
        }

        result.put("added", addedTasksLocalIds);
        result.put("updated", updatedTasksGlobalIds);
        taskDbHelper.close();
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
