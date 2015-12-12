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
        View conditon1EventsRow = findViewById(R.id.condition_1_events);
        final View conditon1Details = TaskCardActivity.this.findViewById(R.id.condition_1_details);
        conditon1Details.setVisibility(View.GONE);
        conditon1EventsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (conditon1Details.getVisibility() == View.VISIBLE) {
                    collapseConditionDetails(conditon1Details);
                } else {
                    expandConditionsDetails(conditon1Details);
                }
            }
        });

        MapFragment.newInstance(this, R.id.mapContainer, new LatLng(-33.867, 151.206));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.task_card, menu);
        return true;
    }

    public static void expandConditionsDetails(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapseConditionDetails(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
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

    //private View makeEventView(Event event) {

    //}
}

