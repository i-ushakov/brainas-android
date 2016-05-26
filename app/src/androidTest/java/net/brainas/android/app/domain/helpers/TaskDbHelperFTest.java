package net.brainas.android.app.domain.helpers;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.support.test.InstrumentationRegistry.getTargetContext;

/**
 * Created by innok on 5/23/2016.
 */

@RunWith(AndroidJUnit4.class)
public class TaskDbHelperFTest extends InstrumentationTestCase {
    private static final String TEST_FILE_PREFIX = "test_";
    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TasksManager tasksManager;
    private TaskChangesDbHelper taskChangesDbHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        /*RenamingDelegatingContext context
                = new RenamingDelegatingContext(getTargetContext(), TEST_FILE_PREFIX);*/

        Context context = getTargetContext();
        appDbHelper = new AppDbHelper(context);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        taskChangesDbHelper = new TaskChangesDbHelper(appDbHelper);
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, 1);
    }

    @After
    public void tearDown() throws Exception {
        super.setUp();

        /*RenamingDelegatingContext context
                = new RenamingDelegatingContext(getTargetContext(), TEST_FILE_PREFIX);*/

        getTargetContext().deleteDatabase(AppDbHelper.DATABASE_NAME);
        appDbHelper.close();
    }

    @Test
    public void deletedAllConditions() {
        Task task = new Task(1, "Message");
        tasksManager.saveTask(task);
        task.setGlobalId(1);
        CopyOnWriteArrayList<Condition> conditions = new CopyOnWriteArrayList<Condition>();
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
        task.setConditions(conditions);
        tasksManager.saveTask(task);

        Task actualTask = tasksManager.getTaskByGlobalId(1);
        assertEquals(2, actualTask.getConditions().size());
        taskDbHelper.deletedAllConditions(actualTask);
        actualTask = tasksManager.getTaskByGlobalId(1);
        assertEquals(0, actualTask.getConditions().size());
    }
}
