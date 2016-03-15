package net.brainas.android.app.activities.taskedit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.UI.views.taskedit.ConditionEditView;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Task;

import java.util.ArrayList;


/**
 * Created by Kit Ushakov on 28/02/2016.
 */
public class EditConditionsActivity extends EditTaskActivity implements Task.TaskChangesObserver {

    private Toolbar toolbar;
    private BrainasApp app;

    private Task task = null;

    LinearLayout conditionsPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_conditions);
        app = (BrainasApp) (BrainasApp.getAppContext());

        long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
        task = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager().getTaskByLocalId(taskLocalId);
        task.attachObserver(this);
        
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderContent();
    }

    public void addCondition(View view) {
        UIHelper.addClickEffectToButton(view, EditConditionsActivity.this);
        Intent intent = new Intent(this, EditEventActivity.class);
        intent.putExtra("taskLocalId", task.getId());
        startActivity(intent);
    }

    public void saveTask(View view) {
        if (UIHelper.safetyBtnClick(view, EditConditionsActivity.this)) {
            task.save();
            showTaskErrorsOrWarnings(task);
            finish();
        }
    }

    public void back(View view) {
        if (UIHelper.safetyBtnClick(view, EditConditionsActivity.this)) {
            task.save();
            Intent intent = new Intent(this, EditDescriptionActivity.class);
            intent.putExtra("taskLocalId", task.getId());
            startActivity(intent);
            finish();
        }
    }

    private void renderContent() {
        ArrayList<Condition> conditions = task.getConditions();
        conditionsPanel.removeAllViews();
        for (Condition condition : conditions) {
            conditionsPanel.addView(new ConditionEditView(this, condition));
        }
    }

    @Override
    public void updateAfterTaskWasChanged() {
        renderContent();
    }
}

