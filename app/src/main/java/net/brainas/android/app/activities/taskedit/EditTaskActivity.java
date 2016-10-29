package net.brainas.android.app.activities.taskedit;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.content.FileProvider;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.CLog;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Image;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.googleDriveApi.GoogleDriveManager;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by innok on 12/7/2015.
 */
public class EditTaskActivity extends AppCompatActivity {

    static public String IMAGE_REQUEST_EXTRA_FIELD_NAME = "image_name";

    static private String TAG ="#$#EDIT_TASK_ACTIVITY#$#";
    static private int GET_IMAGE_FROM_REQUEST = 1001;
    static private final int REQUEST_IMAGE_CAPTURE = 1002;
    static private final int REQUEST_IMAGE_FROM_GALLERY = 1003;



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
    private Image previousPicture = null;
    private Long taskLocalId = null;
    private File photoFile = null;

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

        AccountsManager accountManager = app.getAccountsManager();
        if (accountManager != null) {
            userId = app.getAccountsManager().getCurrentAccountId();
        } else {
            CLog.e(TAG, "We try to get accountManager, but it's null. It seems very strange", null);
            return;
        }

        if (getIntent().hasExtra("taskLocalId")) {
            mode = Mode.EDIT;
            taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
            task = tasksManager.getTaskByLocalId(taskLocalId);
            if (task == null) {
                finish();
            }
        } else {
            task = new Task(userId, getResources().getString(R.string.activity_edit_new_task));
            task.setStatus(Task.STATUSES.TODO);
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
        if (tabLayout.getSelectedTabPosition() == 0) {
            checkTitleFocus();
        }
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
                hideKeyboard();
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
            startActivityForResult(intent, GET_IMAGE_FROM_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_IMAGE_FROM_REQUEST) {
            if (resultCode == RESULT_OK) {
                String imageFileName = data.getStringExtra(IMAGE_REQUEST_EXTRA_FIELD_NAME);
                addPictureToTask(imageFileName);
            }
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                File internalPictureLocation = InfrustructureHelper.createFileInDir(
                        InfrustructureHelper.getPathToImageFolder(((BrainasApp)BrainasApp.getAppContext()).getAccountsManager().getCurrentAccountId()),
                        "task_picture", "jpg",
                        false, false);
                InfrustructureHelper.moveFile(photoFile, internalPictureLocation);
                addPictureToTask(internalPictureLocation.getName());
            } catch (IOException e) {
                e.printStackTrace();
                CLog.e(TAG, "Cannot move file with photo to iternal location", e);
            }
        }

        if (requestCode == REQUEST_IMAGE_FROM_GALLERY && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String[] proj = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(uri,
                    proj, null, null, null);

            String filePath = null;
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                filePath = cursor.getString(column_index);
            }
            cursor.close();

            Bitmap bitmap = BitmapFactory.decodeFile(filePath);

            File internalPictureLocation = null;
            String ext = filePath.substring(filePath.lastIndexOf("."));
            if (!ext.toLowerCase().equals(".jpg") && !ext.toLowerCase().equals(".jpeg") && !ext.toLowerCase().equals(".png") && !ext.toLowerCase().equals(".bmp")) {
                Toast.makeText(EditTaskActivity.this, "Cannot get image of this type", Toast.LENGTH_SHORT).show();
                CLog.i(TAG, "cannot get image of tyep " + ext);
                return;
            }
            try {
                internalPictureLocation = InfrustructureHelper.createFileInDir(
                        InfrustructureHelper.getPathToImageFolder(((BrainasApp)BrainasApp.getAppContext()).getAccountsManager().getCurrentAccountId()),
                        "task_picture", ext,
                        false, false);
                InfrustructureHelper.copyFile(new File(filePath), internalPictureLocation);
                addPictureToTask(internalPictureLocation.getName());
                EditTaskActivity.this.renderPicture();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addPictureToTask(String pictureFileName) {
        Bitmap imageBitmap = InfrustructureHelper.getTaskPicture(pictureFileName, app.getAccountsManager().getCurrentAccountId());
        Image picture = new Image(pictureFileName, imageBitmap);
        task = getTask(taskLocalId);
        Image currentPicture = task.getPicture();
        if (currentPicture != null) {
            if (needToRemoveImage) {
                InfrustructureHelper.removePicture(currentPicture, app.getAccountsManager().getCurrentAccountId());
            } else {
                previousPicture = currentPicture;
            }
        }
        task.setPicture(picture);

        needToRemoveImage = true;

        picture.attachObserver(new Image.ImageDownloadedObserver() {
            @Override
            public void onImageDownloadCompleted() {
                tasksManager.saveTask(task, false, false);
            }
        });

        GoogleDriveManager.getInstance(app).uploadPicture(picture);
    }

    public void capturePicture(View view) {
        if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
            CLog.i(TAG, "Click for capture picture by camera");
            takePhotoIntent();
        }
    }

    public void loadFromGallery(View view) {
        if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
            CLog.i(TAG, "Click for get picture from gallery");
            getFromGalleryIntent();
        }
    }

    protected Task getTask(Long taskLocalId) {
        if (taskLocalId != null && taskLocalId != 0) {
            Task taskFromCache = app.getTasksManager().getTaskByLocalId(taskLocalId);
            if (taskFromCache != null) {
                return taskFromCache;
            }
        }
        return task;
    }

    private void takePhotoIntent() {
        if (!InfrustructureHelper.isExternalStorageWritable()) {
            CLog.e(TAG, "We can't take a picture cause external storage is not writable", null);
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = InfrustructureHelper.createFileInDir(
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + "/",
                        "task_picture", "jpg",
                        false, false
                );

            } catch (IOException e) {
                e.printStackTrace();
                CLog.e(TAG, "Cannot create file for photo from camera", e);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "net.brainas.android.app.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // hack from http://stackoverflow.com/questions/18249007/how-to-use-support-fileprovider-for-sharing-content-to-other-apps
                List<ResolveInfo> resInfoList = EditTaskActivity.this.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    EditTaskActivity.this.grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void getFromGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, REQUEST_IMAGE_FROM_GALLERY);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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
        checkTitleFocus();
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

    private void checkTitleFocus() {
        Editable taskTitleEditable = editTitleField.getText();
        if ((task != null && (task.getMessage() == null || task.getMessage().equals("New task"))) &&
                (taskTitleEditable != null && (taskTitleEditable.toString().trim().matches("") || taskTitleEditable.toString().trim().matches("New task")))) {
            if(editTitleField.requestFocus()) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        } else {
            if(editTitleField.requestFocus()) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            }
        }
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
        if (previousPicture != null) {
            InfrustructureHelper.removePicture(previousPicture, app.getAccountsManager().getCurrentAccountId());
            previousPicture = null;
        }
        Editable taskTitleEditable = editTitleField.getText();
        if (validate()) {
            String message = taskTitleEditable.toString().trim();
            if (task == null) {
                task = new Task(userId, message);
                task.setStatus(Task.STATUSES.TODO);
            }
            if (!message.equals("")) {
                task.setMessage(message);
            }
            task = getTask(taskLocalId);
            tasksManager.saveTask(task);
            tasksManager.addToMappedTasks(task);
            taskLocalId = task.getId();
            showTaskErrorsOrWarnings(task);
        }
    }

    public void cancel(View view) {
        if (UIHelper.safetyBtnClick(view, EditTaskActivity.this)) {
            if (needToRemoveImage && task.getPicture() != null) {
                InfrustructureHelper.removePicture(task.getPicture(), app.getAccountsManager().getCurrentAccountId());
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

