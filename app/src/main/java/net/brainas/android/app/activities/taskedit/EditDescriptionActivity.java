package net.brainas.android.app.activities.taskedit;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.UserAccount;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by innok on 12/7/2015.
 */
public class EditDescriptionActivity extends AppCompatActivity implements Task.TaskChangesObserver {

    private Toolbar toolbar;
    private BrainasApp app;
    private TasksManager tasksManager;
    private AccountsManager accountsManager;
    private UserAccount userAccount;


    private TabLayout tabLayout;
    private ViewGroup descriptionPanel;
    private ViewGroup linkedObjectsPanel;
    private LinearLayout saveTaskBtn;
    private EditText editDescriptionField;

    private String validationError = "";
    private Task task = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_description);

        app = (BrainasApp) (BrainasApp.getAppContext());
        tasksManager = app.getTasksManager();
        long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
        task = tasksManager.getTaskByLocalId(taskLocalId);
        if (task == null) {
            finish();
        }
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

        tabLayout = (TabLayout) findViewById(R.id.sectionsTabs);
        tabLayout.addTab(tabLayout.newTab().setText("DESCRIPTION"), 0, true);
        tabLayout.addTab(tabLayout.newTab().setText("LINKED OBJECTS"), 1);
        setTabLayoutListeners(tabLayout);

        descriptionPanel = (ViewGroup) findViewById(R.id.taskDescriptionPanel);
        linkedObjectsPanel = (ViewGroup) findViewById(R.id.taskLinkedObjectPanel);
        editDescriptionField = (EditText) findViewById(R.id.editDescriptionField);
        saveTaskBtn = (LinearLayout) findViewById(R.id.saveTaskBtn);
        //setSaveBtnOnClickListener();

        refreshPanel();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    private void setTabLayoutListeners(TabLayout tabLayout) {
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        refreshPanel();
                        break;
                    case 1:
                        refreshPanel();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void refreshPanel() {
        switch (tabLayout.getSelectedTabPosition()) {
            case 0:
                descriptionPanel.setVisibility(View.VISIBLE);
                linkedObjectsPanel.setVisibility(View.GONE);
                break;
            case 1:
                descriptionPanel.setVisibility(View.GONE);
                linkedObjectsPanel.setVisibility(View.VISIBLE);
                break;
            default:
                TabLayout.Tab tab = tabLayout.getTabAt(0);
                tab.select();
                break;
        }
        editDescriptionField.setText(task.getDescription());
    }

    public void saveTask(View view) {
        if (UIHelper.safetyBtnClick(view, EditDescriptionActivity.this)) {
            Editable taskTitleEditable = editDescriptionField.getText();
            if ((taskTitleEditable != null && !taskTitleEditable.toString().trim().matches(""))) {
                int userId = app.getAccountsManager().getCurrentAccountId();
                String description = taskTitleEditable.toString().trim();
                task.setDescription(description);
            }
            tasksManager.saveTask(task);
            showTaskErrorsOrWarnings(task);
            finish();
        }
    }

    public void nextToConditionsActivity(View view) {
        if (UIHelper.safetyBtnClick(view, EditDescriptionActivity.this)) {
            Editable taskTitleEditable = editDescriptionField.getText();
            if ((taskTitleEditable != null && !taskTitleEditable.toString().trim().matches(""))) {
                String description = taskTitleEditable.toString().trim();
                task.setDescription(description);
            }

            tasksManager.saveTask(task);
            Intent intent = new Intent(this, EditConditionsActivity.class);
            intent.putExtra("taskLocalId", task.getId());
            startActivity(intent);
            finish();
        }
    }

    private void showTaskErrorsOrWarnings(Task task) {
        Iterator it = task.getWarnings().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry)it.next();
            Toast.makeText(EditDescriptionActivity.this, pair.getValue(), Toast.LENGTH_LONG).show();
        }
    }

    public void back(View view) {
        if (UIHelper.safetyBtnClick(view, EditDescriptionActivity.this)) {
            tasksManager.saveTask(task);
            Intent intent = new Intent(this, EditTaskActivity.class);
            intent.putExtra("taskLocalId", task.getId());
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void updateAfterTaskWasChanged() {
        refreshPanel();
    }

    @Override
    protected void onDestroy() {
        this.task.detachObserver(this);
        super.onDestroy();
    }
}

