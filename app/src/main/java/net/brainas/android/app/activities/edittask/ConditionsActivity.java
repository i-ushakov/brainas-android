package net.brainas.android.app.activities.edittask;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.views.taskedit.ConditionEditView;
import net.brainas.android.app.activities.EditTaskActivity;
import net.brainas.android.app.activities.EditTaskDescriptionActivity;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Task;

import java.util.ArrayList;


/**
 * Created by Kit Ushakov on 28/02/2016.
 */
public class ConditionsActivity extends EditTaskActivity {

    private Toolbar toolbar;
    private BrainasApp app;

    private Task task = null;

    LinearLayout conditionsPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task_conditions);
        app = (BrainasApp) (BrainasApp.getAppContext());

        long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
        task = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager().getTaskByLocalId(taskLocalId);
        
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

        conditionsPanel = (LinearLayout) findViewById(R.id.taskConditionsPanel);

        renderContent();
    }

    public void addCondition(View view) {
        Intent intent = new Intent(this, ConditionActivity.class);
        startActivity(intent);
    }

    public void saveTask(View view) {
        task.setStatus(Task.STATUSES.WAITING);
        task.save();
        showTaskErrorsOrWarnings(task);
        finish();
    }

    public void back(View view) {
        task.setStatus(Task.STATUSES.WAITING);
        task.save();
        Intent intent = new Intent(this, EditTaskDescriptionActivity.class);
        intent.putExtra("taskLocalId", task.getId());
        startActivity(intent);
        finish();
    }

    private void renderContent() {
        ArrayList<Condition> conditions = task.getConditions();
        for (Condition condition : conditions) {
            conditionsPanel.addView(new ConditionEditView(this, condition));
        }
    }
}

