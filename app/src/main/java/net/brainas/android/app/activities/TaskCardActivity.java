package net.brainas.android.app.activities;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.views.taskcard.ConditionBlockView;
import net.brainas.android.app.UI.views.taskcard.EventRowView;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.fragments.MapFragment;

import java.util.ArrayList;

/**
 * Created by innok on 12/7/2015.
 */
public class TaskCardActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_card);

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
        toolbar.inflateMenu(R.menu.task_card);

        Bundle b = getIntent().getExtras();
        long taskId = b.getLong("taskId");
        TasksManager taskManadger = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager();
        Task task = taskManadger.getTaskById(taskId);
        this.task = task;

        fillTheCardWithTaskInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.task_card, menu);
        return true;
    }

    private void fillTheCardWithTaskInfo() {
        // message
        TextView taskMessage = (TextView)findViewById(R.id.task_message);
        taskMessage.setText(task.getMessage());

        // description
        TextView taskDescription = (TextView)findViewById(R.id.task_description);
        taskDescription.setText(task.getDescription());

        // conditions
        ViewGroup conditionsCont = (ViewGroup)findViewById(R.id.task_card_conditions);
        ArrayList<Condition> conditions = task.getConditions();
        for(Condition condition : conditions) {
            LinearLayout conditionBlock = new ConditionBlockView(this, condition);
            conditionsCont.addView(conditionBlock);
        }
    }
}

