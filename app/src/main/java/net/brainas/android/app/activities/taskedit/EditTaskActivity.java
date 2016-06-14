package net.brainas.android.app.activities.taskedit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.BasicImageDownloader;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by innok on 12/7/2015.
 */
public class EditTaskActivity extends AppCompatActivity {
    static private String TAG ="EditTaskActivity";
    static private String IMG_DOWNLOAD_TAG ="DOWNLOADING_IMAGES";

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
        Boolean isActive = existActivities.get(this.getClass().getName());
        if (isActive != null && isActive == true) {
            finish();
        }
        existActivities.put(this.getClass().getName(), true);
        setContentView(R.layout.activity_edit_task);

        app = (BrainasApp) (BrainasApp.getAppContext());
        tasksManager = app.getTasksManager();
        taskHelper = app.getTaskHelper();

        if (getIntent().hasExtra("taskLocalId")) {
            mode = Mode.EDIT;
            long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
            task = tasksManager.getTaskByLocalId(taskLocalId);
            if (task == null) {
                finish();
            }
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

                // TODO Move to another class
                WebView googleSearchWebView = (WebView)findViewById(R.id.googleSearchWebView);
                googleSearchWebView.setWebViewClient(new GoogleSearchWebViewClient());
                googleSearchWebView.loadUrl("https://www.google.ru/search?q=hacker&tbm=isch");
                this.registerForContextMenu(googleSearchWebView);
                googleSearchWebView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        WebView.HitTestResult hr = ((WebView)v).getHitTestResult();
                        if (hr.getType() == WebView.HitTestResult.IMAGE_TYPE){
                            Toast.makeText(EditTaskActivity.this, "IMAGE_TYPE", Toast.LENGTH_SHORT).show();
                        };
                        Toast.makeText(EditTaskActivity.this, "onLongClick", Toast.LENGTH_SHORT).show();
                        BasicImageDownloader basicImageDownloader = new BasicImageDownloader(new BasicImageDownloader.OnImageLoaderListener() {
                            @Override
                            public void onError(BasicImageDownloader.ImageError error) {

                            }

                            @Override
                            public void onProgressChange(int percent) {
                                // TODO set progress mProgress.setProgress(percent);
                                Toast.makeText(EditTaskActivity.this, "onProgressChange " + percent, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onComplete(Bitmap result) {
                                final File imageFile;
                                try {
                                    imageFile = InfrustructureHelper.createFileInDir(
                                            InfrustructureHelper.PATH_TO_TASK_IMAGES_FOLDER,
                                            "task_img", "png",
                                            false, false
                                    );
                                    BasicImageDownloader.writeToDisk(imageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                                        @Override
                                        public void onBitmapSaved() {
                                            // set Task pictire name
                                            // TODO close activity
                                            task.setImage(imageFile.getName());
                                            Toast.makeText(EditTaskActivity.this, "Image was saved", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                                            Toast.makeText(EditTaskActivity.this, "Cannot save image for task ", Toast.LENGTH_SHORT).show();
                                            Log.e(IMG_DOWNLOAD_TAG, "Cannot save image on disk");
                                        }
                                    }, Bitmap.CompressFormat.PNG, false);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(EditTaskActivity.this, "Cannot save image for task ", Toast.LENGTH_SHORT).show();
                                    Log.e(IMG_DOWNLOAD_TAG, "Cannot save image on disk");
                                }

                                Toast.makeText(EditTaskActivity.this, "onComplete", Toast.LENGTH_SHORT).show();
                            }
                        });

                        basicImageDownloader.download(hr.getExtra(), true);
                        return true;
                    }
                });
                googleSearchWebView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(EditTaskActivity.this, "onClick", Toast.LENGTH_SHORT).show();
                    }
                });
                googleSearchWebView.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        WebView.HitTestResult hr = ((WebView)v).getHitTestResult();
                        //if (v.getId() == R.id.web && event.getAction() == MotionEvent.ACTION_DOWN){
                            //handler.sendEmptyMessageDelayed(CLICK_ON_WEBVIEW, 500);
                            //Toast.makeText(EditTaskActivity.this, Integer.toString(hr.getType()), Toast.LENGTH_SHORT).show();
                        //}
                        return false;
                    }
                });
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

      /*  if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            // Menu options for an image.
            //set the header title to the image url
            menu.setHeaderTitle(result.getExtra());
            //menu.add(0, ID_SAVEIMAGE, 0, "Save Image").setOnMenuItemClickListener(handler);
            //menu.add(0, ID_VIEWIMAGE, 0, "View Image").setOnMenuItemClickListener(handler);
        } else if (result.getType() == WebView.HitTestResult.ANCHOR_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            // Menu options for a hyperlink.
            //set the header title to the link url
            menu.setHeaderTitle(result.getExtra());
           // menu.add(0, ID_SAVELINK, 0, "Save Link").setOnMenuItemClickListener(handler);
           // menu.add(0, ID_SHARELINK, 0, "Share Link").setOnMenuItemClickListener(handler);
        }*/
    }

    private class GoogleSearchWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("q=hacker") && url.contains("tbm=isch")) {
                view.loadUrl(url);
                return true;
            } else {
                Toast.makeText(EditTaskActivity.this, "This action is no avalibale", Toast.LENGTH_SHORT).show();
                return true;
            }
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

