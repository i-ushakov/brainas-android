package net.brainas.android.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.views.taskcard.ConditionBlockView;
import net.brainas.android.app.activities.taskedit.EditTaskActivity;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.InfrustructureHelper;
import net.brainas.android.app.infrustructure.SynchronizationManager;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by innok on 12/7/2015.
 */
public class TaskCardActivity extends AppCompatActivity
        implements ActivationManager.ActivationObserver, SynchronizationManager.TaskSyncObserver, Task.TaskChangesObserver {

    private static String TAG = "TaskCardActivity";

    private Toolbar toolbar;
    private Task task;
    private long taskId;
    private TasksManager tasksManager;
    private ViewGroup conditionsCont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_card);

        tasksManager = ((BrainasApp) BrainasApp.getAppContext()).getTasksManager();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Bundle b = getIntent().getExtras();
        this.taskId = b.getLong("taskId");
        task = tasksManager.getTaskByLocalId(taskId);
        if (this.task == null) {
            finish();
            return;
        }
        this.task.attachObserver(this);

        setTaskStatus();
        fillTheCardWithTaskInfo();

        ((BrainasApp)BrainasApp.getAppContext()).getActivationManager().attach(this);
        ((BrainasApp)BrainasApp.getAppContext()).getSynchronizationManager().attach(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        BrainasApp.activityResumed();
        setTaskStatus();
        invalidateOptionsMenu();
        fillTheCardWithTaskInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        BrainasApp.activityPaused();
    }

    //@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(task == null) {
            return true;
        }
        Task.STATUSES status = task.getStatus();

        if (status == Task.STATUSES.ACTIVE) {
            getMenuInflater().inflate(R.menu.task_card_active, menu);
        } else if (status == Task.STATUSES.DONE || status == Task.STATUSES.CANCELED) {
            getMenuInflater().inflate(R.menu.task_card_used, menu);
        } else if (status == Task.STATUSES.WAITING || status == Task.STATUSES.DISABLED) {
            getMenuInflater().inflate(R.menu.task_card_waiting, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_done_task:
                tasksManager.changeStatus(task,Task.STATUSES.DONE);
                setTaskStatus(Task.STATUSES.DONE);
                invalidateOptionsMenu();
                return true;
            case R.id.action_cancel_task:
                tasksManager.changeStatus(task,Task.STATUSES.CANCELED);
                setTaskStatus(Task.STATUSES.CANCELED);
                invalidateOptionsMenu();
                return true;
            case R.id.action_remove_task :
                tasksManager.removeTask(task);
                finish();
                return true;
            case R.id.action_restore_task :
                if (tasksManager.restoreTaskToWaiting(task.getId())) {
                    setTaskStatus(Task.STATUSES.WAITING);
                    invalidateOptionsMenu();
                }
                return true;
            case R.id.action_edit_task :
                Intent tasksIntent = new Intent(TaskCardActivity.this, EditTaskActivity.class);
                tasksIntent.putExtra("mode",EditTaskActivity.Mode.EDIT.toString());
                tasksIntent.putExtra("taskLocalId", task.getId());
                startActivity(tasksIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateAfterActivation() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                updateTaskCard();
            }
        });
    }

    public void updateAfterSync() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                updateTaskCard();
            }
        });
    }

    @Override
    public void updateAfterTaskWasChanged() {
        //updateTaskCard();
    }

    private void updateTaskCard() {
        setTaskStatus();
        invalidateOptionsMenu();
        fillTheCardWithTaskInfo();
    }

    private void fillTheCardWithTaskInfo() {
        if (task == null) {
            return;
        }
        // message
        TextView taskMessage = (TextView)findViewById(R.id.task_message);
        taskMessage.setText(task.getMessage());

        // image
        renderTaskPicture();

        // description
        TextView taskDescription = (TextView)findViewById(R.id.task_description);
        taskDescription.setText(task.getDescription());

        // conditions
        conditionsCont = (ViewGroup)findViewById(R.id.task_card_conditions);
        conditionsCont.removeAllViews();
        CopyOnWriteArrayList<Condition> conditions = task.getConditions();
        for(Condition condition : conditions) {
            LinearLayout conditionBlock = new ConditionBlockView(this, condition);
            conditionsCont.addView(conditionBlock);
        }

        View detailedCardOfTask = findViewById (R.id.detailed_card_of_task);
        detailedCardOfTask.invalidate();
    }

    private void renderTaskPicture() {
        final ImageView taskCardImage = (ImageView)findViewById(R.id.task_card_image);
        if (task != null && task.getPicture() != null) {
            taskCardImage.setAlpha(1f);
            ((View)taskCardImage.getParent()).post(new Runnable() {
                @Override
                public void run() {
                    taskCardImage.setImageBitmap(InfrustructureHelper.getTaskImage(TaskCardActivity.this.task));
                }
            });
        }
    }

    private void setTaskStatus() {
        if (task != null && task.getStatus() != null) {
            String statusLable = task.getStatus().getLabel(this);
            setTitle(statusLable);
        }
    }

    private void setTaskStatus(Task.STATUSES status) {
        String statusLable = status.getLabel(this);
        setTitle(statusLable);
    }

    @Override
    protected void onDestroy() {
        if (conditionsCont != null) {
            conditionsCont.removeAllViews();
        }
        ((BrainasApp)BrainasApp.getAppContext()).getActivationManager().detach(this);
        ((BrainasApp)BrainasApp.getAppContext()).getSynchronizationManager().detach(this);
        if (this.task != null) {
            this.task.detachObserver(this);
        }
        super.onDestroy();
    }

}

