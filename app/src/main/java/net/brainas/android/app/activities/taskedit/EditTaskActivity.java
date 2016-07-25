package net.brainas.android.app.activities.taskedit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Image;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.googleDriveApi.GoogleDriveManager;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by innok on 12/7/2015.
 */
public class EditTaskActivity extends AppCompatActivity {

    static public String IMAGE_REQUEST_EXTRA_FIELD_NAME = "image_name";

    static private String TAG ="EditTaskActivity";
    static private int GET_IMAGE_REQUEST = 1001;


    static HashMap<String, Boolean> existActivities = new HashMap<>();

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
    private ImageView taskPictureView;
    private EditText editTitleField;
    private ViewGroup categoryPanel;
    private ViewGroup conditionPanel;
    private LinearLayout saveTaskBtn;


    private String validationErrorMessage = "";
    private Mode mode;
    private Task task = null;
    private int userId;
    private boolean needToRemoveImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Boolean isActive = existActivities.get(this.getClass().getName());
        if (isActive != null && isActive == true) {
            finish();
        }
        existActivities.put(this.getClass().getName(), true);
        setContentView(R.layout.activity_edit_task);

        app = (BrainasApp) (BrainasApp.getAppContext());
        tasksManager = app.getTasksManager();
        taskHelper = app.getTaskHelper();

        userId = app.getAccountsManager().getCurrentAccountId();

        if (getIntent().hasExtra("taskLocalId")) {
            mode = Mode.EDIT;
            long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
            task = tasksManager.getTaskByLocalId(taskLocalId);
            if (task == null) {
                finish();
            }
        } else {
            task = new Task(userId, getResources().getString(R.string.activity_edit_new_task));
            task.setStatus(Task.STATUSES.DISABLED);
            mode = Mode.CREATE;
        }
        setToolBar();
        setTabLayout(new ArrayList<>(Arrays.asList(tabs)));

        taskTitlePanel = (ViewGroup) findViewById(R.id.taskTitlePanel);
        taskPicturePanel = (ViewGroup) findViewById(R.id.taskPicturePanel);
        taskPictureView = (ImageView)findViewById(R.id.taskPictureView);
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

    @Override
    protected void onDestroy() {
        existActivities.put(this.getClass().getName(), false);
        super.onDestroy();
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
                categoryPanel.setVisibility(View.VISIBLE);
                conditionPanel.setVisibility(View.VISIBLE);
                taskPicturePanel.setVisibility(View.GONE);
                break;
            case 1:
                taskTitlePanel.setVisibility(View.GONE);
                categoryPanel.setVisibility(View.GONE);
                conditionPanel.setVisibility(View.GONE);
                taskPicturePanel.setVisibility(View.VISIBLE);
                renderPicture();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        //WebView.HitTestResult result = view.getHitTestResult();

        MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                // do the menu action
                return true;
            }
        };
    }

    public void searchImageInInternet(View view) {
        if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
            Intent intent = new Intent(EditTaskActivity.this, SearchPictureActivity.class);
            Editable taskTitleEditable = editTitleField.getText();
            if (taskTitleEditable != null) {
                intent.putExtra("searchTerm", taskTitleEditable.toString());
            }
            startActivityForResult(intent, GET_IMAGE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                String imageFileName = data.getStringExtra(IMAGE_REQUEST_EXTRA_FIELD_NAME);
                Bitmap imageBitmap = InfrustructureHelper.getTaskPicture(imageFileName, app.getAccountsManager().getCurrentAccountId());
                Image picture = new Image(imageFileName, imageBitmap);
                task.setPicture(picture);
                needToRemoveImage = true;
                GoogleDriveManager.getInstance(app).uploadPicture(picture);
            }
        }
    }

    public void capturePicture(View view) {
        UIHelper.addClickEffectToButton(view, this);
    }

    public void loadFromGallery(View view) {
        UIHelper.addClickEffectToButton(view, this);
    }

    private void renderPicture() {
        if (task != null && task.getPicture() != null) {
            taskPictureView.setAlpha(1f);
            taskPicturePanel.post(new Runnable() {
                @Override
                public void run() {
                    taskPictureView.setImageBitmap(InfrustructureHelper.getTaskPicture(
                            EditTaskActivity.this.task.getPicture().getName(),
                            app.getAccountsManager().getCurrentAccountId()));
                }
            });
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
        if ((taskTitleEditable == null || taskTitleEditable.toString().trim().matches("")) && !task.haveImage()) {
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


    private void save() {
        needToRemoveImage = false;
        Editable taskTitleEditable = editTitleField.getText();
        if (validate()) {
            String message = taskTitleEditable.toString().trim();
            if (task == null) {
                task = new Task(userId, message);
                task.setStatus(Task.STATUSES.WAITING);
            }
            if (!message.equals("")) {
                task.setMessage(message);
            }
            tasksManager.saveTask(task);
            tasksManager.addToMappedTasks(task);
            showTaskErrorsOrWarnings(task);
        }
    }

    public void cancel(View view) {
        if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
            if (needToRemoveImage && task.getPicture() != null) {
                // TODO remove image
                needToRemoveImage= false;
            }
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

