package net.brainas.android.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.Toast;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.views.TaskTileView;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.SyncManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasksActivity extends AppCompatActivity implements SyncManager.TaskSyncObserver {
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private GridView tasksGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

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
        tabLayout.addTab(tabLayout.newTab().setText("ACTIVE"), 1);
        setTabLayoutListeners(tabLayout);

        tasksGrid = (GridView) findViewById(R.id.tasks_grid);
        updateTasksGrid(TasksManager.GROUP_OF_TASKS.ALL);

        SyncManager.getInstance().attach(this);
    }

    private void updateTasksGrid(TasksManager.GROUP_OF_TASKS group) {
        tasksGrid.setAdapter(new TaskTileAdapter(this, group));
        tasksGrid.setOnItemClickListener(null);
        tasksGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText(TasksActivity.this, "" + position,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void updateAfterSync() {
        TasksManager.GROUP_OF_TASKS group;
        switch (tabLayout.getSelectedTabPosition()) {
            case 0 :
                group = TasksManager.GROUP_OF_TASKS.ALL;
                break;
            case 1 :
                group = TasksManager.GROUP_OF_TASKS.ACTIVE;
                break;
            default:
                group = TasksManager.GROUP_OF_TASKS.ALL;
                TabLayout.Tab tab = tabLayout.getTabAt(0);
                tab.select();
                break;
        }
        updateTasksGrid(group);
    }

    private class TaskTileAdapter extends BaseAdapter {
        private Context context;
        private List<Task> tasks = new ArrayList<Task>();

        public TaskTileAdapter(Context context, TasksManager.GROUP_OF_TASKS group) {
            this.context = context;
            Map<String,Object> params = new HashMap<>();
            params.put("GROUP_OF_TASKS", group);
            TasksManager taskManager = ((BrainasApp)(BrainasApp.getAppContext())).getTasksManager();
            tasks = taskManager.getTasksFromDB(params);
        }

        public int getCount() {
            return tasks.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        public TaskTileView getView(int position, View convertView, ViewGroup parent) {
            TaskTileView taskTileView;
            if (convertView == null) {
                Task task = tasks.get(position);
                //taskTileView = new TaskTileView(context, null, 0, task);
                //taskTileView.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.MATCH_PARENT));
                //taskTileView.setPadding(8, 8, 8, 8);
            } else {
                taskTileView = (TaskTileView) convertView;
            }
            //return taskTileView;
            return null;
        }
    }

    private void setTabLayoutListeners(TabLayout tabLayout) {
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        updateTasksGrid(TasksManager.GROUP_OF_TASKS.ALL);
                        break;
                    case 1:
                        updateTasksGrid(TasksManager.GROUP_OF_TASKS.ACTIVE);
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

}
