package net.brainas.android.app.domain.helpers;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;

/**
 * Created by innok on 5/23/2016.
 */

@RunWith(AndroidJUnit4.class)
public class TaskManagerFTest extends InstrumentationTestCase {
    private static final String TEST_FILE_PREFIX = "test_";
    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TasksManager taskManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Context context = getTargetContext();

        appDbHelper = new AppDbHelper(context);
        TaskChangesDbHelper taskChangesDbHelper = new TaskChangesDbHelper(appDbHelper);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        taskManager = new TasksManager(taskDbHelper, taskChangesDbHelper,1);

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
    public void deleteTaskByGlobalId() {
        Task task = new Task(1, "Test 1");
        task.setGlobalId(11);
        taskManager.saveTask(task);
        taskManager.deleteTaskByGlobalId(12);
        taskManager.deleteTaskByGlobalId(0);
        taskManager.deleteTaskByGlobalId(-1);
        Task actualTask = taskManager.getTaskByGlobalId(11);
        assertEquals(task,actualTask);
        taskManager.deleteTaskByGlobalId(11);
        actualTask = taskManager.getTaskByGlobalId(11);
        assertNull(actualTask);
    }
}
