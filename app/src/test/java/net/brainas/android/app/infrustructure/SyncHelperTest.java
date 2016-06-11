package net.brainas.android.app.infrustructure;

import android.support.v4.util.Pair;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Task;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Created by Kit Ushakov on 2/15/2016.
 */

public class SyncHelperTest {
    private static String TAG = "SyncHelperTest";
    private static Document xmlDocumentFromServer = null;
    private static Integer accountId = 1;
    private static String expectedAccessToken = "aCcE-ss_TokeN";
    private static String expectedInitSyncTime = "27-04-2016 11:00:23";

    // params for task1 from server
    private static String task1Message = "Task message 1 ";
    private static String task1Description = "Task description 1";
    private static String task1Status = "WAITING";
    private static int task1GlobalId = 11;
    private static String task1TimeOfChanges = "25-04-2016 11:20:00";
    private static int condition1GlobalId = 111;
    private static int event1Id= 9001;
    private static Double event1Lat = 55.599165616703;
    private static Double event1Lng = 38.125401735306;
    private static Double event1Radius = 100d;
    private static String event1Address = "ул. Фрунзе,  12,  Жуковский";

    // params for task2 from server
    private static String task2Message = "Task message 2 ";
    private static String task2Description = "Task description 2";
    private static String task2Status = "WAITING";
    private static int task2GlobalId = 12;
    private static String task2TimeOfChanges = "26-04-2016 11:20:00";
    private static int condition21GlobalId = 21;
    private static int event211Id= 211;
    private static Integer event211Offset = 180;
    private static String event211Datetime = "21-04-2016 15:00:12";

    //params for synchronized task3 from server
    private static Long task3SynchronizedFromServerLocalId = 3l;
    private static Long task3SynchronizedFromServerGlobalId = 13l;

    //params for synchronized task4 from server
    private static Long task4SynchronizedFromServerLocalId = 4l;
    private static Long task4SynchronizedFromServerGlobalId = 14l;

    //params for synchronized (was deleted on local) task5 from server
    private static Long task5SynchronizedFromServerLocalId = 0l;
    private static Long task5SynchronizedFromServerGlobalId = 15l;

    //params for synchronized condition6 from server
    private static long condition6SynchronizedFromServerLocalId = 61l;
    private static long condition6SynchronizedFromServerGlobalId = 601l;

    //params for synchronized event7 from server
    private static long event7SynchronizedFromServerLocalId = 71;
    private static long event7SynchronizedFromServerGlobalId = 701;

    SyncHelper syncHelper;


    @Mock
    TasksManager tasksManager;
    @Mock
    TaskChangesDbHelper taskChangesDbHelper;
    @Mock
    TaskDbHelper taskDbHelper;
    @Mock
    ServicesDbHelper servicesDbHelper;
    @Mock
    UserAccount userAccount;
    @Mock
    AccountsManager accountsManager;
    @Mock
    Task task, task3;
    @Mock
    Condition condition;
    @Mock
    EventLocation eventLocation;
    @Mock
    EventLocation eventTime;

    @BeforeClass
    public static void beforeSyncHelperTest() {
        xmlDocumentFromServer = createXmlDocumentFromServer();
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(accountsManager.getUserAccount()).thenReturn(userAccount);
        when(userAccount.getId()).thenReturn(1);
        syncHelper = new SyncHelper(tasksManager,taskChangesDbHelper, taskDbHelper, userAccount, accountsManager);
    }

    @Test
    public void getAllChangesInXMLTest() throws Exception {
        Pair<String, String> pair = new Pair<String, String>("UPDATED", "2016-02-10 11:20:33");
        System.out.println(pair.first);
        HashMap<Long, Pair<String, String>> changes = new HashMap<Long, Pair<String, String>>();
        changes.put(1l, pair);

        when(taskChangesDbHelper.getChangedTasks(accountId)).thenReturn(changes);
        when(tasksManager.getTaskByLocalId(anyInt())).thenReturn(task);

        // task
        when(task.getId()).thenReturn((long) 1);
        when(task.getGlobalId()).thenReturn((long) 1);
        when(task.getMessage()).thenReturn("Message of tasks");
        when(task.getDescription()).thenReturn("Descritpion of task");
        CopyOnWriteArrayList<Condition> conditions = new CopyOnWriteArrayList <Condition>();
        conditions.add(condition);
        when(task.getConditions()).thenReturn(conditions);

        // condition
        when(condition.getId()).thenReturn(10l);
        when(condition.getGlobalId()).thenReturn(200);
        ArrayList<Event> events = new ArrayList<>();
        events.add(eventLocation);
        events.add(eventTime);
        when(condition.getEvents()).thenReturn(events);

        // eventLocation
        when(eventLocation.getId()).thenReturn(901l);
        when(eventLocation.getGlobalId()).thenReturn(9001);
        when(eventLocation.getType()).thenReturn(Event.TYPES.GPS);
        when(eventLocation.getJSONStringWithParams()).thenReturn("{\"address\":\"ул. Фрунзе,  12,  Жуковский\",\"radius\":100,\"lng\":38.125401735306,\"lat\":55.599165616703}");

        // eventTime
        when(eventTime.getId()).thenReturn(902l);
        when(eventTime.getGlobalId()).thenReturn(9002);
        when(eventTime.getType()).thenReturn(Event.TYPES.TIME);
        when(eventTime.getJSONStringWithParams()).thenReturn("{\"offset\":180,\"datetime\":\"20-04-2016 14:38:41\"}");

        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(task);
        when(tasksManager.getAllTasks()).thenReturn(tasks);

        String actual  = syncHelper.getAllChangesInXML(accountId);
        System.out.println(actual);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<changes>" +
                "<existingTasks>{\"1\":\"1\"}</existingTasks>" +
                    "<changedTasks>" +
                        "<changedTask globalId=\"1\" id=\"1\">" +
                            "<message>Message of tasks</message>" +
                            "<description>Descritpion of task</description>" +
                            "<conditions>" +
                                "<condition globalId=\"200\" localId=\"10\">" +
                                    "<events>" +
                                        "<event globalId=\"9001\" localId=\"901\">" +
                                            "<type>GPS</type>" +
                                            "<params>{\"address\":\"ул. Фрунзе,  12,  Жуковский\",\"radius\":100,\"lng\":38.125401735306,\"lat\":55.599165616703}</params>" +
                                        "</event>" +
                                        "<event globalId=\"9002\" localId=\"902\">" +
                                            "<type>TIME</type>" +
                                            "<params>{\"offset\":180,\"datetime\":\"20-04-2016 14:38:41\"}</params>" +
                                        "</event>" +
                                    "</events>" +
                                "</condition>" +
                            "</conditions>" +
                            "<change>" +
                                "<status>UPDATED</status>" +
                                "<changeDatetime>2016-02-10 11:20:33</changeDatetime>" +
                            "</change>" +
                        "</changedTask>" +
                    "</changedTasks>" +
                "</changes>";
        System.out.println(expected);
        assertEquals("XML with changes is wrong", expected, actual);
    }

    @Test
    public void retriveSynchronizedObjectsTest() {
        when(tasksManager.getTaskByLocalId(3)).thenReturn(task3);
        when(tasksManager.getTaskByLocalId(4)).thenReturn(null);

        JSONObject synchronizedObjects = syncHelper.retriveSynchronizedObjects(xmlDocumentFromServer);
        try {
            ArrayList<Pair<Long,Long>> actualSynchronizedTasks = (ArrayList<Pair<Long,Long>>)synchronizedObjects.get("synchronizedTasks");
            ArrayList<Pair<Long,Long>> actualSynchronizedConditions = (ArrayList<Pair<Long,Long>>)synchronizedObjects.get("synchronizedConditions");
            ArrayList<Pair<Long,Long>> actualSynchronizedEvents = (ArrayList<Pair<Long,Long>>)synchronizedObjects.get("synchronizedEvents");

            ArrayList<Pair<Long,Long>> expectedSynchronizedTasks = new  ArrayList<>();
            expectedSynchronizedTasks.add(new Pair<Long, Long>(task3SynchronizedFromServerLocalId, task3SynchronizedFromServerGlobalId));
            expectedSynchronizedTasks.add(new Pair<Long, Long>(task4SynchronizedFromServerLocalId, task4SynchronizedFromServerGlobalId));
            expectedSynchronizedTasks.add(new Pair<Long, Long>(task5SynchronizedFromServerLocalId, task5SynchronizedFromServerGlobalId));

            ArrayList<Pair<Long,Long>> expectedSynchronizedConditions = new  ArrayList<>();
            expectedSynchronizedConditions.add(new Pair<Long, Long>(condition6SynchronizedFromServerLocalId, condition6SynchronizedFromServerGlobalId));

            ArrayList<Pair<Long,Long>> expectedSynchronizedEvents = new  ArrayList<>();
            expectedSynchronizedEvents.add(new Pair<Long, Long>(event7SynchronizedFromServerLocalId, event7SynchronizedFromServerGlobalId));

            Assert.assertEquals(expectedSynchronizedTasks, actualSynchronizedTasks );
            Assert.assertEquals(expectedSynchronizedConditions, actualSynchronizedConditions);
            Assert.assertEquals(expectedSynchronizedEvents, actualSynchronizedEvents);
        } catch (JSONException e) {
            e.printStackTrace();
            fail("JSONException occurred while parse syncObject result");
        }
    }

    @Test
    public void retrieveTimeOfInitialSyncTates() {
        String actualInitSyncTime = syncHelper.retrieveTimeOfInitialSync(xmlDocumentFromServer);
        assertEquals("InitSyncTime is wrong", actualInitSyncTime, "27-04-2016 11:00:23");
    }

    @Test
    public void checkTheRelevanceOfTheChangesTest_serverTimeLater() {
        long globalId = 1;
        String timeOfServerChanges = "2016-04-26 11:20:30";

        when(tasksManager.getTaskByGlobalId(anyInt())).thenReturn(task);
        when(task.getId()).thenReturn(1l);

        when(taskChangesDbHelper.getTimeOfLastChanges(anyLong())).thenReturn("2016-04-20 11:20:30");
        boolean isRelevantChanges = syncHelper.checkTheRelevanceOfTheChanges(globalId, timeOfServerChanges);
        boolean expected = true;
        assertEquals("Problem with detect time relevance of changes", expected, isRelevantChanges);
    }

    @Test
    public void checkTheRelevanceOfTheChangesTest_serverTimeTheSame() {
        long globalId = 1;
        String timeOfServerChanges = "2016-04-20 11:24:33";

        when(tasksManager.getTaskByGlobalId(anyInt())).thenReturn(task);
        when(task.getId()).thenReturn(1l);

        when(taskChangesDbHelper.getTimeOfLastChanges(anyLong())).thenReturn("2016-04-20 11:24:33");
        boolean isRelevantChanges = syncHelper.checkTheRelevanceOfTheChanges(globalId, timeOfServerChanges);
        boolean expected = false;
        assertEquals("Problem with detect time relevance of changes", expected, isRelevantChanges);
    }

    @Test
    public void checkTheRelevanceOfTheChangesTest_serverTimeEarly() {
        long globalId = 1;
        String timeOfServerChanges = "2016-04-19 14:00:00";

        when(tasksManager.getTaskByGlobalId(anyInt())).thenReturn(task);
        when(task.getId()).thenReturn(1l);

        when(taskChangesDbHelper.getTimeOfLastChanges(anyLong())).thenReturn("2016-04-20 11:24:33");
        boolean isRelevantChanges = syncHelper.checkTheRelevanceOfTheChanges(globalId, timeOfServerChanges);
        boolean expected = false;
        assertEquals("Problem with detect time relevance of changes", expected, isRelevantChanges);
    }

    @Test
    public void retrieveAccessTokenTest() {
        String expectedAccessToken = "aCcE-ss_TokeN";
        String accessToken = syncHelper.retrieveAccessToken(xmlDocumentFromServer);
        assertEquals("The access token is not the one that was expected", expectedAccessToken, accessToken);
    }

    @Test
    public void retrieveAndSaveTasksFromServerTest () {
        int accountId = 1;
        ArrayList<Task> updatedTasks = syncHelper.retrieveAndSaveTasksFromServer(xmlDocumentFromServer);

        // create expected Task1
        ArrayList<Task> expected = new ArrayList<>();
        Task task1 = new Task(accountId, task1Message);
        task1.setGlobalId(task1GlobalId);
        task1.setDescription(task1Description);
        task1.setStatus(task1Status);
        CopyOnWriteArrayList<Condition> conditions1 = new CopyOnWriteArrayList<Condition>();
        Condition condition11 =  new Condition(null, condition1GlobalId, task1.getId());
        EventLocation event111 = new EventLocation(null, event1Id, null);
        event111.setLat(event1Lat);
        event111.setLng(event1Lng);
        event111.setRadius(event1Radius);
        event111.setAddress(event1Address);
        condition11.addEvent(event111);
        conditions1.add(condition11);
        task1.setConditions(conditions1);
        expected.add(task1);


        // create expected Task2
        Task task2 = new Task(accountId, task2Message);
        task2.setGlobalId(task2GlobalId);
        task2.setDescription(task2Description);
        task2.setStatus(task2Status);
        CopyOnWriteArrayList<Condition> conditions2 = new CopyOnWriteArrayList<Condition>();
        Condition condition21 =  new Condition(null, condition21GlobalId, task2.getId());
        EventTime event211 = new EventTime(null, event211Id, null);
        event211.setDatetime(event211.getCalendarFromString(event211Datetime, event211Offset));
        condition21.addEvent(event211);
        conditions2.add(condition21);
        task2.setConditions(conditions2);
        expected.add(task2);

        assertEquals("Resulting arrays with updated tasks is different size", expected.size(), updatedTasks.size());
        for (int i = 0; i < expected.size() ; i++) {
            assertTrue(updatedTasks.get(i).equals(expected.get(i)));
        }
    }

    @Test
    public void retrieveDeletedTasksFromServerTest() {
        // xmlDocument
        String xmlStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<syncResponse><tasks>" +
                    "<deleted>" +
                        "<deletedTask global-id='11' time-changes='25-04-2016 11:20:00'></deletedTask>" +
                        "<deletedTask global-id='12' time-changes='24-04-2016 11:25:00'></deletedTask>" +
                    "</deleted>" +
                "</tasks></syncResponse>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlStr));
            Document xmlDocument = null;
            xmlDocument = builder.parse(is);

            // tagName
            String tagName = "deletedTask";
            ArrayList<Integer> deletedTasks = syncHelper.retrieveDeletedTasksFromServer(xmlDocument, tagName);

            ArrayList<Integer> expected = new ArrayList<Integer>();
            expected.add(11);
            expected.add(12);

            assertEquals("Wrong ids array of deleted tasks", expected, deletedTasks);

        } catch (JSONException|SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fixture mthoid
     * @return xmlDocumentFormServer
     */
    private static Document createXmlDocumentFromServer() {
        // xmlDocument got from server
        String xmlStr =
                "<syncResponse>" +
                    "<tasks>" +
                        "<created>" +
                            "<task global-id='" + task1GlobalId + "' time-changes='" + task1TimeOfChanges + "'>" +
                                "<message>" + task1Message + "</message>" +
                                "<description>" + task1Description + "</description>" +
                                "<conditions>" +
                                    "<condition id='" + condition1GlobalId + "' task-id='11'>" +
                                        "<event id=\"" + event1Id + "\" type='GPS'>" +
                                            "<params>" +
                                                "<address>" + event1Address + "</address>" +
                                                "<radius>" + event1Radius + "</radius>" +
                                                "<lng>" + event1Lng + "</lng>" +
                                                "<lat>" + event1Lat + "</lat>" +
                                            "</params>" +
                                        "</event>" +
                                    "</condition>" +
                                "</conditions>" +
                                "<status>" + task1Status + "</status>" +
                            "</task>" +
                            "<task global-id='" + task2GlobalId + "' time-changes='" + task2TimeOfChanges + "'>" +
                                "<message>" + task2Message + "</message>" +
                                "<description>" + task2Description + "</description>" +
                                "<conditions>" +
                                    "<condition id='" + condition21GlobalId + "' task-id='11'>" +
                                    "<event id=\"" + event211Id + "\" type='TIME'>" +
                                        "<params>" +
                                            "<datetime>" + event211Datetime + "</datetime>" +
                                            "<offset>" + event211Offset + "</offset>" +
                                        "</params>" +
                                    "</event>" +
                                    "</condition>" +
                                "</conditions>" +
                                "<status>" + task1Status + "</status>" +
                            "</task>" +
                        "</created>" +
                    "</tasks>" +
                    "<synchronizedObjects>" +
                        "<synchronizedTasks>" +
                            "<synchronizedTask>" +
                                "<localId>" + task3SynchronizedFromServerLocalId + "</localId>" +
                                "<globalId>" + task3SynchronizedFromServerGlobalId + "</globalId>" +
                            "</synchronizedTask>" +
                            "<synchronizedTask>" +
                                "<localId>" + task4SynchronizedFromServerLocalId + "</localId>" +
                                "<globalId>" + task4SynchronizedFromServerGlobalId + "</globalId>" +
                            "</synchronizedTask>" +
                            "<synchronizedTask>" +
                                "<localId>" + task5SynchronizedFromServerLocalId + "</localId>" +
                                "<globalId>" + task5SynchronizedFromServerGlobalId + "</globalId>" +
                            "</synchronizedTask>" +
                        "</synchronizedTasks>" +
                        "<synchronizedConditions>" +
                            "<synchronizedCondition>" +
                                "<localId>" + condition6SynchronizedFromServerLocalId + "</localId>" +
                                "<globalId>" + condition6SynchronizedFromServerGlobalId + "</globalId>" +
                            "</synchronizedCondition>" +
                        "</synchronizedConditions>" +
                        "<synchronizedEvents>" +
                            "<synchronizedEvent>" +
                                "<localId>" + event7SynchronizedFromServerLocalId + "</localId>" +
                                "<globalId>" + event7SynchronizedFromServerGlobalId + "</globalId>" +
                            "</synchronizedEvent>" +
                        "</synchronizedEvents>" +
                    "</synchronizedObjects>" +
                    "<initSyncTime>" + expectedInitSyncTime + "</initSyncTime>" +
                    "<accessToken>" + expectedAccessToken + "</accessToken>" +
                "</syncResponse>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        InputSource is = new InputSource(new StringReader(xmlStr));
        Document xmlDocumentFromServer = null;
        try {
            xmlDocumentFromServer = builder.parse(is);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xmlDocumentFromServer;
    }

    @Test
    public void handleResponseFromServerTest() {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<syncResponse>" +
                "<tasks>" +
                    "<created></created>" +
                    "<updated>" +
                        "<task global-id=\"838\" time-changes=\"2016-05-09 10:16:04\"><message>1</message><description></description><conditions><condition id='341' task-id='838'><event type='GPS' id='411'><params><address>Красноярский край,  Россия,  662131</address><radius>100</radius><lng>89.736328125</lng><lat>57.231502991479</lat></params></event></condition></conditions><status>WAITING</status></task>" +
                        "<task global-id=\"839\" time-changes=\"2016-05-09 10:58:43\"><message>2</message><description></description><conditions></conditions><status>WAITING</status></task>" +
                        "<task global-id=\"840\" time-changes=\"2016-05-09 11:00:07\"><message>3</message><description></description><conditions></conditions><status>WAITING</status></task>" +
                    "</updated>" +
                    "<deleted>" +
                        "<deletedTask global-id=\"833\" time-changes=\"2016-05-08 19:30:11\"></deletedTask>" +
                        "<deletedTask global-id=\"832\" time-changes=\"2016-05-08 19:30:12\"></deletedTask>" +
                    "</deleted>" +
                "</tasks>" +
                "<synchronizedObjects>" +
                    "<synchronizedTasks>" +
                        "<synchronizedTask>" +
                            "<localId>" + task3SynchronizedFromServerLocalId + "</localId>" +
                            "<globalId>" + task3SynchronizedFromServerGlobalId + "</globalId>" +
                        "</synchronizedTask>" +
                        "<synchronizedTask>" +
                            "<localId>" + task4SynchronizedFromServerLocalId + "</localId>" +
                            "<globalId>" + task4SynchronizedFromServerGlobalId + "</globalId>" +
                        "</synchronizedTask>" +
                    "<synchronizedTask>" +
                        "<localId>" + task5SynchronizedFromServerLocalId + "</localId>" +
                        "<globalId>" + task5SynchronizedFromServerGlobalId + "</globalId>" +
                    "</synchronizedTask>" +
                "</synchronizedTasks>" +
                "<synchronizedConditions>" +
                    "<synchronizedCondition>" +
                        "<localId>" + condition6SynchronizedFromServerLocalId + "</localId>" +
                        "<globalId>" + condition6SynchronizedFromServerGlobalId + "</globalId>" +
                    "</synchronizedCondition>" +
                "</synchronizedConditions>" +
                "<synchronizedEvents>" +
                    "<synchronizedEvent>" +
                        "<localId>" + event7SynchronizedFromServerLocalId + "</localId>" +
                        "<globalId>" + event7SynchronizedFromServerGlobalId + "</globalId>" +
                    "</synchronizedEvent>" +
                "</synchronizedEvents>" +
                "</synchronizedObjects>" +
                "<initSyncTime>2016-05-09 11:04:59</initSyncTime>" +
                "<accessToken>{\"access_token\":\"ya29.CjndAmGQy7PP9Ku1XDysVz7gYBnzltRqFG_h69ot_GAMSdIxH56vMI-GlSJxpNq-JzjWVVwFSPpWxi4\",\"token_type\":\"Bearer\",\"expires_in\":3593,\"id_token\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IjVmZWIxNGI5MjhiZjdjODc5ZjcwOGIxNWU3OTZmYTk2NzFkZWRiZDcifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhdF9oYXNoIjoiWE1jNnJxTmdacWRFcW5TTE1hVEVYQSIsImF1ZCI6IjkyNTcwNTgxMTMyMC1jZW5icWcxZmU1amI4MDQxMTZvZWZsNzhzYmlzaG5nYS5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInN1YiI6IjExNzQzMDE0MDk4NjA1ODg2ODM5OCIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhenAiOiI5MjU3MDU4MTEzMjAtY2VuYnFnMWZlNWpiODA0MTE2b2VmbDc4c2Jpc2huZ2EuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJlbWFpbCI6ImtpdHVzaGFrb2ZmQGdtYWlsLmNvbSIsImlhdCI6MTQ2Mjc5MTg5OSwiZXhwIjoxNDYyNzk1NDk5LCJuYW1lIjoiS2l0IFVzaGFrb3YiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDQuZ29vZ2xldXNlcmNvbnRlbnQuY29tLy1nM0dYNFctUXFKSS9BQUFBQUFBQUFBSS9BQUFBQUFBQUFCQS9DbmtyeE4xUzJ3MC9zOTYtYy9waG90by5qcGciLCJnaXZlbl9uYW1lIjoiS2l0IiwiZmFtaWx5X25hbWUiOiJVc2hha292IiwibG9jYWxlIjoiZW4ifQ.gl5blgXCUvmUAtqGh4jaCasVdHjuoDOO9VtDR8Xq8HUjg5ksIaztgQR5Rg7NPJXPjvg7-nZBfUwCBSICQ5cfODlcMcRCVsqaX43fT4Y13Wa40ZnzHmg0mN2YFeuVW5A8ZSyWX-Folkd5IpXV0ETxndxklAmiyYsYpTv4DuZLCClMf0EQBHhUWopR-8fgVxLPKf1sVn5CghXtEskmK4s0h-6Kt2UMnK5y5JuZpxF5NEX-yM1VjVEIh6T_o1XldQKyjBJbzBQ4OOJVq9DOh5O_aydrppATWoFWwJCh9F4RRYb6NhPOoWLHhm0a7BEN6zaDRqW1VdxZgsoHf_Qia69ERw\",\"created\":1462791899,\"refresh_token\":\"1\\/-k_L3G2sWGkcTIrzc67TuzgxFwThzUhGCGSGymJeSPA\"}</accessToken></syncResponse>\n";

        when(accountsManager.saveUserAccount(any(UserAccount.class))).thenReturn(true);
        syncHelper.handleResponseFromServer(response);

        verify(tasksManager, times(3)).saveTask(any(Task.class));
        verify(tasksManager, times(2)).deleteTaskByGlobalId(anyInt());
        verify(tasksManager, times(2)).getTaskByLocalId(anyLong());
        verify(tasksManager, times(2)).getTaskByLocalId(anyLong());
        verify(taskChangesDbHelper, times(1)).removeFromSync(task5SynchronizedFromServerGlobalId);
        verify(taskChangesDbHelper, times(1)).removeFromSync(task4SynchronizedFromServerGlobalId);
        verify(taskChangesDbHelper, times(1)).removeFromSync(task3SynchronizedFromServerGlobalId);
        verify(taskDbHelper, times(1)).setConditionGlobalId(condition6SynchronizedFromServerLocalId, condition6SynchronizedFromServerGlobalId);
        verify(taskDbHelper, times(1)).setEventGlobalId(event7SynchronizedFromServerLocalId, event7SynchronizedFromServerGlobalId);
    }

    @Test
    public void getAllExistingTasksWithGlobalId() {
        ArrayList<Task> tasks = new ArrayList<>();
        Task task1 = new Task(1, "Test 1");
        task1.setId(1);
        task1.setGlobalId(11);
        tasks.add(task1);

        Task task2 = new Task(1, "Test 2");
        task2.setId(2);
        task2.setGlobalId(22);
        tasks.add(task2);

        Task task3 = new Task(1, "Test 3");
        task3.setId(3);
        task3.setGlobalId(0);
        tasks.add(task3);
        when(tasksManager.getAllTasks()).thenReturn(tasks);

        JSONObject expected = new JSONObject();
        try {
            expected.put("11","1");
            expected.put("22","2");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject actual = syncHelper.getAllExistingTasksWithGlobalId();
        try {
            Assert.assertEquals(expected.get("11"), actual.get("11"));
            Assert.assertEquals(expected.get("22"), actual.get("22"));
            Assert.assertEquals(expected.toString(), actual.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
