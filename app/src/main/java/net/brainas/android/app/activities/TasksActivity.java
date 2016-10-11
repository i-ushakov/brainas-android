package net.brainas.android.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.views.TaskTileView;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.synchronization.SynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasksActivity extends AppCompatActivity implements
        SynchronizationManager.TaskSyncObserver,
        ActivationManager.ActivationObserver,
        Task.TaskChangesObserver,
        SearchView.OnQueryTextListener {
    private BrainasApp app;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private GridView tasksGrid;
    private TextView userNotSignedInMessage;
    private String searchText = null;
    private TaskTileAdapter taskTileAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        app = ((BrainasApp)BrainasApp.getAppContext());
        setTitle("Tasks");

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

        tabLayout = (TabLayout) findViewById(R.id.taskTabs);
        tabLayout.addTab(tabLayout.newTab().setText("ALL"), 0, true);
        tabLayout.addTab(tabLayout.newTab().setText("WAITING"), 1);
        tabLayout.addTab(tabLayout.newTab().setText("USED"), 2);
        setTabLayoutListeners(tabLayout);

        tasksGrid = (GridView) findViewById(R.id.tasks_grid);
        userNotSignedInMessage = (TextView) findViewById(R.id.user_not_signed_in_message);

        app.getSynchronizationManager().attach(this);
        app.getActivationManager().attach(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BrainasApp.activityResumed();
        refreshTaskGrid();
    }

    @Override
    public void onPause() {
        super.onPause();
        BrainasApp.activityPaused();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.bar_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        this.searchText = newText;
        if (newText.equals("")) {
            this.searchText = null;
        }
        refreshTaskGrid();
        return false;
    }

    private void updateTasksGrid(HashMap<String,Object> params, int accountId) {
        userNotSignedInMessage.setVisibility(View.GONE);
        if (tasksGrid.getAdapter() == null) {
            taskTileAdapter = new TaskTileAdapter(this, params, accountId);
            tasksGrid.setAdapter(taskTileAdapter);
        } else {
            taskTileAdapter.updateItems(params, accountId);
            tasksGrid.invalidateViews();
        }
        tasksGrid.setOnItemClickListener(null);
        tasksGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Intent taskCardIntent = new Intent(TasksActivity.this, TaskCardActivity.class);
                Bundle b = new Bundle();
                b.putLong("taskId", id);
                taskCardIntent.putExtras(b);
                startActivity(taskCardIntent);
            }
        });
    }

    @Override
    public void updateAfterSync() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTaskGrid();
            }
        });
    }

    public void updateAfterActivation() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTaskGrid();
            }
        });
    }

    public void updateAfterTaskWasChanged() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                //refreshTaskGrid();
            }
        });
    }

    private void refreshTaskGrid() {
        if (app.getAccountsManager().isUserSingIn()) {
            HashMap<String,Object> params = new HashMap<String, Object>();
            TasksManager.GROUP_OF_TASKS group;
            switch (tabLayout.getSelectedTabPosition()) {
                case 0:
                    group = TasksManager.GROUP_OF_TASKS.ALL;
                    break;
                case 1:
                    group = TasksManager.GROUP_OF_TASKS.WAITING;
                    break;
                case 2:
                    group = TasksManager.GROUP_OF_TASKS.USED;
                    break;
                default:
                    group = TasksManager.GROUP_OF_TASKS.ALL;
                    TabLayout.Tab tab = tabLayout.getTabAt(0);
                    tab.select();
                    break;
            }
            params.put(TaskDbHelper.GROUP_OF_TASK_PARAM, group);
            if (this.searchText != null) {
                params.put(TaskDbHelper.SEARCH_TEXT_PARAM, this.searchText);
            }
            int accountId = app.getAccountsManager().getUserAccount().getId();
            updateTasksGrid(params, accountId);
        } else {
            showUserNotSignedInMessage();
        }
    }

    private class TaskTileAdapter extends BaseAdapter {
        private Context context;
        private List<Task> tasks = new ArrayList<Task>();

        public TaskTileAdapter(Context context, HashMap<String, Object> params, int accountId) {
            this.context = context;
            tasks.clear();
            tasks.addAll(app.getTasksManager().getTasksFromDB(params, accountId));
            /*for (Task task : tasks) {
                task.attachObserver(TasksActivity.this);
            }*/
        }

        public int getCount() {
            return tasks.size();
        }

        public Object getItem(int position) {
            return tasks.get(position);
        }

        public long getItemId(int position) {
            return tasks.get(position).getId();
        }

        public TaskTileView getView(int position, View convertView, ViewGroup parent) {
            TaskTileView taskTileView;
           // if (convertView == null) {
                Task task = tasks.get(position);
                taskTileView = new TaskTileView(context, task);
                taskTileView.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.MATCH_PARENT));
                taskTileView.setPadding(8, 8, 8, 8);
            //} else {
                //taskTileView = (TaskTileView) convertView;
            //}
            return taskTileView;
        }

        public void updateItems(HashMap<String, Object> params, int accountId) {
            tasks.clear();
            tasks.addAll(app.getTasksManager().getTasksFromDB(params, accountId));
            this.notifyDataSetChanged();
        }
    }

    private void setTabLayoutListeners(TabLayout tabLayout) {
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        refreshTaskGrid();
                        break;
                    case 1:
                        refreshTaskGrid();
                        break;
                    case 2:
                        refreshTaskGrid();
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

    private void showUserNotSignedInMessage() {
        userNotSignedInMessage.setVisibility(View.VISIBLE);
        tasksGrid.setAdapter(null);
        tasksGrid.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        app.getSynchronizationManager().detach(this);
        app.getActivationManager().detach(this);
        ArrayList<Task> tasks = app.getTasksManager().getAllTasksFromHeap();
        for (Task task : tasks) {
            task.detachObserver(TasksActivity.this);
        }
        super.onDestroy();
    }
}
