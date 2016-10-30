package net.brainas.android.app.activities.taskedit;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.UI.views.taskedit.ConditionEditView;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Task;

import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Created by Kit Ushakov on 28/02/2016.
 */
public class EditConditionsActivity extends EditTaskActivity implements Task.TaskChangesObserver, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Toolbar toolbar;
    private BrainasApp app;
    private GoogleApiClient mGoogleApiClient;

    private Task task = null;
    private long taskLocalId;

    LinearLayout conditionsPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_conditions);
        app = (BrainasApp) (BrainasApp.getAppContext());

        taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
        task = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager().getTaskByLocalId(taskLocalId);
        if (task == null) {
            finish();
        }
        task.attachObserver(this);
        
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

        conditionsPanel = (LinearLayout) findViewById(R.id.taskConditionsPanel);

        // TODO: need to remove all this stuff below
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderContent();
    }

    @Override
    protected void onDestroy() {
        this.task.detachObserver(this);
        super.onDestroy();
    }

    public void addCondition(View view) {
        UIHelper.addClickEffectToButton(view, EditConditionsActivity.this);
        Intent intent = new Intent(this, EditEventActivity.class);
        intent.putExtra("taskLocalId", task.getId());
        startActivity(intent);
    }

    public void saveTask(View view) {
        if (UIHelper.safetyBtnClick(view, EditConditionsActivity.this)) {
            task = getTask(taskLocalId);
            tasksManager.saveTask(task, true, true, true);
            showTaskErrorsOrWarnings(task);
            finish();
        }
    }

    public void back(View view) {
        if (UIHelper.safetyBtnClick(view, EditConditionsActivity.this)) {
            tasksManager.saveTask(task);
            Intent intent = new Intent(this, EditDescriptionActivity.class);
            intent.putExtra("taskLocalId", task.getId());
            startActivity(intent);
            finish();
        }
    }

    public void removeCondition(View view) {
        if (UIHelper.safetyBtnClick(view, EditConditionsActivity.this)) {
            ViewParent parentView = Utils.findParentRecursively(view, R.id.viewConditionEdit);
            ConditionEditView conditionEditView = (ConditionEditView)parentView.getParent();
            Condition condition = conditionEditView.getCondition();
            task.removeCondition(condition);
            tasksManager.removeGeofence(condition, getApplicationContext(), mGoogleApiClient);
            tasksManager.saveTask(task);
        }
    }


    private void renderContent() {
        CopyOnWriteArrayList<Condition> conditions = task.getConditions();
        conditionsPanel.removeAllViews();
        for (Condition condition : conditions) {
            conditionsPanel.addView(new ConditionEditView(this, condition));
        }
    }

    @Override
    public void updateAfterTaskWasChanged() {
        renderContent();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

