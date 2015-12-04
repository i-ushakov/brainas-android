package net.brainas.android.app.activities;

import android.app.Activity;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.logic.TilesManager;
import net.brainas.android.app.UI.views.TaskTileView;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.SyncManager;


public class MainActivity extends Activity {

    private BrainasApp app;
    private MainActivity.ActivePanel activePanel = ActivePanel.MESSAGES;
    private ViewGroup massagesPanel;
    private View menuPanel;
    private ImageView slideButton;
    private ImageView addTaskButton;

    public enum ActivePanel {
        MESSAGES, GENERAL
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //app = (BrainasApp)this.getApplication();
        //app.setMainActivity(this);

        ViewGroup massagesPanel = (ViewGroup) findViewById(R.id.messages_panel);
        TaskTileView t = new TaskTileView(this);
        massagesPanel.addView(t);

        //Task task = new Task("Meesage");
        //TaskTileView taskTIle = new TaskTileView(this);
        //RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        //params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        //params.setMargins(100, 100, 100, 100);
        //taskTIle.setLayoutParams(params);
        //massagesPanel.addView(taskTIle);
        //massagesPanel.invalidate();

       // TextView t2 =  new TextView(this);
       // t2.setText("333");
        //taskTIle.addView(t2);


        //menuPanel = findViewById(R.id.menu_panel);
        //slideButton = (ImageView)this.findViewById(R.id.slide_button);
        //addTaskButton = (ImageView)this.findViewById(R.id.add_task_button);

        //setOnTouchListenerForSlideButton();
        //setOnClickListenerForTasksMenuItem();
        //initLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    private void setOnClickListenerForTasksMenuItem() {
        ImageView menuItemTasks = (ImageView)this.findViewById(R.id.menu_item_tasks);
        menuItemTasks.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent tasksIntent = new Intent(MainActivity.this, TasksActivity.class);
                startActivity(tasksIntent);
            }
        });
    }

    private void initLayout(){
        final View view = findViewById(R.id.menu_panel);
        view.post(new Runnable() {
            @Override
            public void run() {
                MainActivity m = (MainActivity) view.getContext();
                m.recalculateSlideButtonHeight();
            }
        });

        final View messagesPanel = findViewById(R.id.messages_panel);
        messagesPanel.post(new Runnable() {
            @Override
            public void run() {
                //TilesManager tilesManager = new TilesManager(massagesPanel);
                //Task task = new Task("Meesage");
                //TaskTileView taskTIle = new TaskTileView(massagesPanel.getContext(), null, 0,task);
                //massagesPanel.addView(taskTIle);

                //massagesPanel.postInvalidate();
                //app.setTilesManager(tilesManager);
                //tilesManager.addTilesWithTasks();

                //SyncManager.getInstance().attach(tilesManager);
                //SyncManager.getInstance().startSynchronization();

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
        int distanceY = menuPanel.getHeight() - slideButton.getLayoutParams().height - addTaskButton.getHeight();
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
        layoutParams.height = massagesPanel.getHeight() - (massagesPanel.getWidth()/15) * 20 - addTaskButton.getHeight();
        slideButton.setLayoutParams(layoutParams);
    }
}
