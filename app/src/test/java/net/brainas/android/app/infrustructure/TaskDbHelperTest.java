package net.brainas.android.app.infrustructure;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Task;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;

import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Created by Kit Ushakov on 2/15/2016.
 */

public class TaskDbHelperTest {
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
    ServicesDbHelper servicesDbHelper;
    @Mock
    Condition condition;
    @Mock
    EventLocation eventLocation;
    @Mock
    EventLocation eventTime;
    @Mock
    SQLiteDatabase db;
    @Mock
    Cursor cursor;
    @Mock
    AppDbHelper appDbHelper;


    TasksManager tasksManager;
    TaskDbHelper taskDbHelper, taskDbHelperSpy;
    Task task1;
    CopyOnWriteArrayList<Condition> conditions;
    Condition condition1, condition2;
    EventLocation event11;
    EventTime event21;

    @BeforeClass
    public static void beforeSyncHelperTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, 1);
        when(appDbHelper.getDbAccess()).thenReturn(db);
        when(db.rawQuery(anyString(), any(String[].class))).thenReturn(cursor);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        taskDbHelperSpy = spy(taskDbHelper);

        task1 = new Task(1, "Message");
        task1.setGlobalId(1);
        task1.setId(1);

        conditions = new CopyOnWriteArrayList<Condition>();
        condition1 = new Condition();
        condition1.setId(1);
        event11 = new EventLocation();
        event11.setParent(condition1).setGlobalId(11);
        event11.setLat(1d).setLng(2d).setRadius(100d).setAddress("House of Cards");
        condition1.addEvent(event11);
        conditions.add(condition1);
        task1.setConditions(conditions);
        condition2 = new Condition();
        condition2.setId(2);
        event21 = new EventTime();
        event21.setParent(condition2).setGlobalId(21);
        event21.setDatetime(Calendar.getInstance());
        condition2.addEvent(event21);
        conditions.add(condition2);
        task1.setConditions(conditions);
    }


    @Test public void  addOrUpdateTask() {
        taskDbHelperSpy.addOrUpdateTask(task1);
        verify(taskDbHelperSpy, times(1)).deletedAllConditions(task1);
    }

    @Test public void deletedAllConditions() {
        doReturn(conditions).when(taskDbHelperSpy).getConditions(task1);
        taskDbHelperSpy.deletedAllConditions(task1);
        String selectionForEvents = TaskDbHelper.COLUMN_NAME_EVENTS_CONDITION + " LIKE ?";
        String[] selectionArgsForEvent = { String.valueOf(1) };
        verify(db, times(1)).delete(TaskDbHelper.TABLE_EVENTS, selectionForEvents, selectionArgsForEvent);
        selectionForEvents = TaskDbHelper.COLUMN_NAME_EVENTS_CONDITION + " LIKE ?";
        String[] selectionArgsForEvent2 = { String.valueOf(1) };
        verify(db, times(1)).delete(TaskDbHelper.TABLE_EVENTS, selectionForEvents, selectionArgsForEvent2);
        String selectionForConditions = TaskDbHelper.COLUMN_NAME_CONDITIONS_TASK + " LIKE ?";
        String[]  selectionArgsForConditions = { String.valueOf(task1.getId()) };
        db.delete(TaskDbHelper.TABLE_CONDITIONS, selectionForConditions, selectionArgsForConditions);
    }
}
