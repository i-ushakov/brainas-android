package net.brainas.android.app.activities;

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
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.UserAccount;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by innok on 12/7/2015.
 */
public class EditTaskActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BrainasApp app;
    private AccountsManager accountsManager;
    private UserAccount userAccount;


    private TabLayout tabLayout;
    private ViewGroup taskTitlePanel;
    private ViewGroup taskPicturePanel;
    private RelativeLayout saveTaskBtn;
    private EditText editTitleField;

    private String validationError = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

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
        tabLayout.addTab(tabLayout.newTab().setText("TEXT"), 0, true);
        tabLayout.addTab(tabLayout.newTab().setText("PICTURE"), 1);
        setTabLayoutListeners(tabLayout);

        taskTitlePanel = (ViewGroup) findViewById(R.id.taskTitlePanel);
        taskPicturePanel = (ViewGroup) findViewById(R.id.taskPicturePanel);
        saveTaskBtn = (RelativeLayout) findViewById(R.id.saveTaskBtnInner);
        setSaveBtnOnClickListener();
        editTitleField = (EditText) findViewById(R.id.editTitleField);

        refreshPanel();

        app = (BrainasApp) (BrainasApp.getAppContext());
        //accountActivityTitle = (TextView) findViewById(R.id.account_activity_title);


        /*singInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });*/
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
    }

    private void setSaveBtnOnClickListener() {
        saveTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateTask()) {
                    saveTask();
                } else {
                    Toast.makeText(EditTaskActivity.this, EditTaskActivity.this.validationError, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean validateTask() {
        Editable taskTitleEditable = editTitleField.getText();
        if ((taskTitleEditable == null || taskTitleEditable.toString().trim().matches("")) && !isPictureSet()) {
            this.validationError = "You must input title of task or add picture";
            return false;
        }
        return true;
    }

    private boolean isPictureSet() {
        return false;
    }

    private void saveTask() {
        Editable taskTitleEditable = editTitleField.getText();
        if ((taskTitleEditable != null && !taskTitleEditable.toString().trim().matches(""))) {
            int userId = app.getAccountsManager().getCurrenAccountId();
            String message = taskTitleEditable.toString().trim();
            Task task = new Task(userId , message);
            task.setStatus(Task.STATUSES.WAITING);
            task.save();
            showTaskErrorsOrWarnings(task);
            finish();
        }
    }

    private void showTaskErrorsOrWarnings(Task task) {
        Iterator it = task.getWarnings().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry)it.next();
            Toast.makeText(EditTaskActivity.this, pair.getValue(), Toast.LENGTH_LONG).show();
        }
    }
}

