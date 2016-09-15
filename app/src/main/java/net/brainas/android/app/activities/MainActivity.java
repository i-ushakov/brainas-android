package net.brainas.android.app.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.location.LocationSettingsStates;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.UI.logic.ReminderScreenManager;
import net.brainas.android.app.activities.taskedit.EditTaskActivity;
import net.brainas.android.app.infrustructure.LocationProvider;


public class MainActivity extends AppCompatActivity  implements AccountsManager.ManagePreloader{
    static public final int REQUEST_CHECK_SETTINGS = 1001;

    private BrainasApp app;
    private LocationProvider locationProvider;
    private MainActivity.ActivePanel activePanel = ActivePanel.MESSAGES;
    private ViewGroup massagesPanel;
    private View menuPanel;
    private ImageView slideButton;
    private ImageView addTaskButton;
    private ProgressDialog mProgressDialog;

    public enum ActivePanel {
        MESSAGES, GENERAL
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (BrainasApp)this.getApplication();
        app.setMainActivity(this);

        locationProvider = new LocationProvider(this);
        app.setLocationProvider(locationProvider);

        massagesPanel = (ViewGroup) findViewById(R.id.messages_panel);
        menuPanel = findViewById(R.id.menu_panel);
        slideButton = (ImageView)this.findViewById(R.id.slide_button);
        addTaskButton = (ImageView)this.findViewById(R.id.add_task_button);

        setOnTouchListenerForSlideButton();
        setOnClickListenerForTodoMenuItem();
        setOnClickListenerForTasksMenuItem();
        setOnClickListenerForAccountsMenuItem();
        setOnClickListenerForAddTaskButton();

        if (!app.getAccountsManager().initialSingIn(this)) {
            startAccountsActivity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        BrainasApp.activityResumed();
        setActivePanel(ActivePanel.MESSAGES);
        initLayout();
    }

    @Override
    public void onPause() {
        super.onPause();
        BrainasApp.activityPaused();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showPreloader( ) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(this.getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hidePreloader() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void setOnTouchListenerForSlideButton() {
        slideButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.deepSkyBlue));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.skyBlue));
                    togglePanel();
                }
                return true;
            }
        });
    }

    private void setOnClickListenerForTodoMenuItem() {
        ImageView menuItemTodo = (ImageView)this.findViewById(R.id.menu_item_todo);
        menuItemTodo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (UIHelper.safetyBtnClick(v, MainActivity.this)) {
                    Intent todoIntent = new Intent(MainActivity.this, TodoListActivity.class);
                    startActivity(todoIntent);
                }
            }
        });
    }

    private void setOnClickListenerForTasksMenuItem() {
        ImageView menuItemTasks = (ImageView)this.findViewById(R.id.menu_item_tasks);
        menuItemTasks.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (UIHelper.safetyBtnClick(v, MainActivity.this)) {
                    Intent tasksIntent = new Intent(MainActivity.this, TasksActivity.class);
                    startActivity(tasksIntent);
                }
            }
        });
    }

    private void setOnClickListenerForAccountsMenuItem() {
        ImageView menuItemTasks = (ImageView)this.findViewById(R.id.menu_item_accounts);
        menuItemTasks.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (UIHelper.safetyBtnClick(v, MainActivity.this)) {
                    startAccountsActivity();
                }
            }
        });
    }

    private void startAccountsActivity() {
        Intent tasksIntent = new Intent(MainActivity.this, AccountsActivity.class);
        startActivity(tasksIntent);
    }

    private void setOnClickListenerForAddTaskButton() {
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startEditTaskActivity();
            }
        });
    }

    private void recalculateAddTaskButtonHeight() {
        android.view.ViewGroup.LayoutParams layoutParams = addTaskButton.getLayoutParams();
        int btnHeight = (massagesPanel.getHeight() - (massagesPanel.getWidth()/15) * 20)*65/100;
        layoutParams.height = btnHeight;
        addTaskButton.setLayoutParams(layoutParams);
        addTaskButton.getParent().requestLayout();
    }

    private void startEditTaskActivity() {
        Intent tasksIntent = new Intent(MainActivity.this, EditTaskActivity.class);
        startActivity(tasksIntent);
    }

    private void initLayout(){
        final View view = findViewById(R.id.menu_panel);
        view.post(new Runnable() {
            @Override
            public void run() {
                MainActivity m = (MainActivity) view.getContext();
                m.recalculateAddTaskButtonHeight();
                m.recalculateSlideButtonHeight();
            }
        });

        final View messagesPanel = findViewById(R.id.messages_panel);
        messagesPanel.post(new Runnable() {
            @Override
            public void run() {
                ReminderScreenManager reminderScreenManager = new ReminderScreenManager(massagesPanel);
                app.getAccountsManager().attach(reminderScreenManager);
                app.setReminderScreenManager(reminderScreenManager);
                reminderScreenManager.refreshTilesWithActiveTasks();

                findViewById(R.id.messages_panel).postInvalidate();
                MainActivity m = (MainActivity) view.getContext();
                m.slideDown();
            }
        });
    }

    private MainActivity.ActivePanel togglePanel(){
        switch (getActivePanel()) {
            case MESSAGES :
                slideUp();
                setActivePanel(ActivePanel.GENERAL);
                return ActivePanel.GENERAL;
            case GENERAL :
                slideDown();
                setActivePanel(ActivePanel.MESSAGES);
                return ActivePanel.MESSAGES;
            default:
                return getActivePanel();
        }
    }

    private void setActivePanel(ActivePanel activePanel) {
        this.activePanel = activePanel;
    }

    private ActivePanel getActivePanel() {
        return this.activePanel;
    }

    private void slideDown() {
        int distanceY = menuPanel.getHeight() - slideButton.getLayoutParams().height - addTaskButton.getLayoutParams().height;
        menuPanel.animate().translationY(distanceY);
        slideButton.setImageResource(R.drawable.slide_up_icon);
    }

    private void slideUp() {
        View generalPanel = findViewById(R.id.menu_panel);
        generalPanel.animate().translationY(0);
        ImageView slideButton = (ImageView)this.findViewById(R.id.slide_button);
        slideButton.setImageResource(R.drawable.slide_down_icon);
    }

    private void recalculateSlideButtonHeight() {
        ViewGroup.LayoutParams layoutParams = slideButton.getLayoutParams();
        layoutParams.height = massagesPanel.getHeight() - (massagesPanel.getWidth()/15) * 20 - addTaskButton.getLayoutParams().height;
        slideButton.setLayoutParams(layoutParams);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == AccountsManager.RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (!app.getAccountsManager().handleSignInResult(result, this) || !app.getAccountsManager().isUserSingIn()) {
                startAccountsActivity();
            }
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
            final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
            switch (resultCode) {
                case Activity.RESULT_OK:
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(this,"If you not allow use wifi networks, it may lead to worse determination of your location", Toast.LENGTH_LONG);
                    break;
                default:
                    break;
            }
        }
    }
}
