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
import java.util.Calendar;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Created by Kit Ushakov on 2/15/2016.
 */

public class TasksManagerTest {
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
    TaskChangesDbHelper taskChangesDbHelper;
    @Mock
    TaskDbHelper taskDbHelper;
    @Mock
    Task task, task3;
    @Mock
    UserAccount userAccount;
    @Mock
    AccountsManager accountsManager;
    @Mock
    Condition condition;
    @Mock
    EventLocation eventLocation;
    @Mock
    EventLocation eventTime;
    TasksManager tasksManager;

    @BeforeClass
    public static void beforeSyncHelperTest() {
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, 1);
        when(userAccount.getId()).thenReturn(1);
        syncHelper = new SyncHelper(tasksManager,taskChangesDbHelper);
    }

    @Test
    public void saveTest() {
        Task task1 = new Task(1, "Message");
        task1.setGlobalId(1);

        /*CopyOnWriteArrayList<Condition> conditions = new CopyOnWriteArrayList<Condition>();
        Condition condition1 = new Condition();
        EventLocation event11 = new EventLocation();
        event11.setParent(condition1).setGlobalId(11);
        event11.setLat(1d).setLng(2d).setRadius(100d).setAddress("House of Cards");
        condition1.addEvent(event11);
        conditions.add(condition1);
        task.setConditions(conditions);
        Condition condition2 = new Condition();
        EventTime event21 = new EventTime();
        event21.setParent(condition2).setGlobalId(21);
        event21.setDatetime(Calendar.getInstance());
        condition2.addEvent(event21);
        conditions.add(condition2);
        task.setConditions(conditions);*/

        tasksManager.saveTask(task1);
        verify(taskDbHelper, times(1)).addOrUpdateTask(task1);
    }
}
