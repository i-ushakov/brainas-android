package net.brainas.android.app.activities.taskedit;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by innok on 12/7/2015.
 */
public class EditTaskActivity extends AppCompatActivity {

    public enum Mode {
        CREATE, EDIT
    }

    protected BrainasApp app;
    protected TasksManager tasksManager;
    protected TaskHelper taskHelper;

    String[] tabs = {"TEXT", "PICTURE"};

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewGroup taskTitlePanel;
    private ViewGroup taskPicturePanel;
    private EditText editTitleField;
    private ViewGroup categoryPanel;
    private ViewGroup conditionPanel;
    private LinearLayout saveTaskBtn;


    private String validationErrorMessage = "";
    private Mode mode;
    private Task task = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        app = (BrainasApp) (BrainasApp.getAppContext());
        tasksManager = app.getTasksManager();
        taskHelper = app.getTaskHelper();

        if (getIntent().hasExtra("taskLocalId")) {
            mode = Mode.EDIT;
            long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
            task = tasksManager.getTaskByLocalId(taskLocalId);
        } else {
            mode = Mode.CREATE;
        }
        setToolBar();
        setTabLayout(new ArrayList<>(Arrays.asList(tabs)));

        taskTitlePanel = (ViewGroup) findViewById(R.id.taskTitlePanel);
        taskPicturePanel = (ViewGroup) findViewById(R.id.taskPicturePanel);
        editTitleField = (EditText) findViewById(R.id.editTitleField);
        setEditTitleFieldListeners();
        categoryPanel = (ViewGroup) findViewById(R.id.middleCategoryPanel);
        conditionPanel = (ViewGroup) findViewById(R.id.middleConditionPanel);
        saveTaskBtn = (LinearLayout) findViewById(R.id.saveTaskBtn);
        setSaveBtnOnClickListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BrainasApp.activityResumed();
        refreshContent();
    }

    @Override
    public void onPause() {
        super.onPause();
        BrainasApp.activityPaused();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    protected void setToolBar() {
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
    }

    protected void setTabLayout(ArrayList<String> tabs) {
        tabLayout = (TabLayout) findViewById(R.id.sectionsTabs);
        int i = 0;
        for (String tab : tabs) {
            tabLayout.addTab(tabLayout.newTab().setText(tab), i, i++ == 0 ? true : false);
        }
        setTabLayoutListeners(tabLayout);
    }

    private void refreshContent() {
        validate();
        switch (tabLayout.getSelectedTabPosition()) {
            case 0:
                taskTitlePanel.setVisibility(View.VISIBLE);
                taskPicturePanel.setVisibility(View.GONE);
                break;
            case 1:
                taskTitlePanel.setVisibility(View.GONE);
                taskPicturePanel.setVisibility(View.VISIBLE);
                break;
            default:
                TabLayout.Tab tab = tabLayout.getTabAt(0);
                tab.select();
                break;
        }
        if (mode == Mode.EDIT) {
            editTitleField.setText(task.getMessage());
        }
        if (task != null && task.getConditions().size() > 0) {
            conditionPanel.removeAllViews();
            LinearLayout imagesOfEventTypes = taskHelper.getImagesBlockForConditions(task.getConditions(), this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            conditionPanel.addView(imagesOfEventTypes, lp);
        }
    }

    private void setTabLayoutListeners(TabLayout tabLayout) {
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        refreshContent();
                        break;
                    case 1:
                        refreshContent();
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

    private void setSaveBtnOnClickListener() {
        saveTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
                    if (validate()) {
                        save();
                        finish();
                    } else {
                        Toast.makeText(EditTaskActivity.this, EditTaskActivity.this.validationErrorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void setConditionPanelOnClickListener() {
        conditionPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
                    if (validate()) {
                        save();
                        Intent intent;
                        if (task.getConditions().size() > 0) {
                            intent = new Intent(EditTaskActivity.this, EditConditionsActivity.class);
                        } else {
                            intent = new Intent(EditTaskActivity.this, EditEventActivity.class);
                        }
                        intent.putExtra("taskLocalId", task.getId());
                        startActivity(intent);
                    } else {
                        Toast.makeText(EditTaskActivity.this, EditTaskActivity.this.validationErrorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void setEditTitleFieldListeners() {
        editTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                validate();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });
    }

    private boolean validate() {
        Editable taskTitleEditable = editTitleField.getText();
        if ((taskTitleEditable == null || taskTitleEditable.toString().trim().matches("")) && !isPictureSet()) {
            this.validationErrorMessage = "You must input title of task or add picture";
            conditionPanel.setOnClickListener(null);
            conditionPanel.setAlpha(0.5f);
            categoryPanel.setAlpha(0.5f);
            return false;
        }
        setConditionPanelOnClickListener();
        conditionPanel.setAlpha(1);
        categoryPanel.setAlpha(1);
        return true;
    }

    private boolean isPictureSet() {
        return false;
    }

    private void save() {
        Editable taskTitleEditable = editTitleField.getText();
        if (validate()) {
            int userId = app.getAccountsManager().getCurrentAccountId();
            String message = taskTitleEditable.toString().trim();
            if (task == null) {
                task = new Task(userId, message);
                task.setStatus(Task.STATUSES.WAITING);
            }
            task.setMessage(message);
            tasksManager.saveTask(task);
            tasksManager.addToMappedTasks(task);
            showTaskErrorsOrWarnings(task);
        }
    }

    public void cancel(View view) {
        if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
            finish();
        }
    }

    protected void showTaskErrorsOrWarnings(Task task) {
        Iterator it = task.getWarnings().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry)it.next();
            Toast.makeText(EditTaskActivity.this, pair.getValue(), Toast.LENGTH_LONG).show();
        }
    }

    public void nextToDescriptionActivity(View view) {
        if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
            if (validate()) {
                save();
                Intent intent = new Intent(this, EditDescriptionActivity.class);
                intent.putExtra("taskLocalId", task.getId());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(EditTaskActivity.this, EditTaskActivity.this.validationErrorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}

