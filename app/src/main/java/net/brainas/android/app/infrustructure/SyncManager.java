package net.brainas.android.app.infrustructure;

import android.os.AsyncTask;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.Task;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

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
    static String serverUrl = "http://192.168.1.105/backend/web/connection/";
    //static String serverUrl = "http://brainas.net/backend/web/connection/";
    static String lineEnd = "\r\n";
    static String boundary =  "*****";
    static String xmlOutDirPath = "/app_sync/xml/out";

    private List<TaskSyncObserver> observers = new ArrayList<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> syncThreadHandle = null;


    private SyncManager() {}

    public static synchronized SyncManager getInstance() {
        if(instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    public void startSynchronization() {
        final Runnable syncThread = new Runnable() {
            public void run() {
                synchronization();
            }
        };

        syncThreadHandle =
                scheduler.scheduleAtFixedRate(syncThread, 0, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stopSynchronization() {
        syncThreadHandle.cancel(true);
    }

    public interface TaskSyncObserver {
        void update();
    }

    public void attach(TaskSyncObserver observer){
        observers.add(observer);
    }

    private void synchronization() {
        new AllTasksSync().execute();
    }

    private class AllTasksSync extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... files) {
            File xmlFile;
            String response;
            List<Task> tasks;

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
                tasks = parseResponse(response);
            } catch (IOException | SAXException | ParserConfigurationException e) {
                // Cannot parse xml-document that gotten from server
                e.printStackTrace();
                return null;
            }

            // refreshes tasks in DB
            updateTasksInDb(tasks);

            // notify about fresh tasks
            ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
                public void run() {
                    notifyAllObservers();
                }
            });
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

    private  List<Task> parseResponse(String xmlStr) throws ParserConfigurationException, IOException, SAXException {
        List<Task> tasks = new ArrayList<Task>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlStr));
        Document xmlDocument = builder.parse(is);

        NodeList taskList = xmlDocument.getElementsByTagName("task");

        for (int i = 0; i < taskList.getLength(); ++i) {
            Element taskEl = (Element) taskList.item(i);
            int globalId = Integer.parseInt(taskEl.getAttribute("global-id"));
            String message = taskEl.getElementsByTagName("message").item(0).getTextContent();
            Task task = new Task(message);
            task.setGlobalId(globalId);
            tasks.add(task);
        }

        return tasks;
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
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void notifyAllObservers() {
        for (TaskSyncObserver observer : observers) {
            observer.update();
        }
    }
}
