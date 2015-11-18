package net.brainas.android.app.activities;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.logic.TilesManager;
import net.brainas.android.app.infrustructure.SyncManager;


public class MainActivity extends AppCompatActivity {

    private BrainasApp app;
    private MainActivity.ActivePanel activePanel = ActivePanel.MESSAGES;

    public enum ActivePanel {
        MESSAGES, GENERAL
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (BrainasApp)this.getApplication();
        app.setMainActivity(this);

        setOnTouchListenerForSlideButton();
        initLayout();
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
        ImageView slideButton = (ImageView)this.findViewById(R.id.slide_button);
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

    private void initLayout(){
        final View view = findViewById(R.id.general_panel);
        view.post(new Runnable() {
            @Override
            public void run() {
                MainActivity m = (MainActivity) view.getContext();
                m.slideDown();

                ViewGroup massagesPanel = (ViewGroup) findViewById(R.id.messages_panel);
                TilesManager tilesManager = new TilesManager(massagesPanel);
                tilesManager.addTilesWithTasks();

                SyncManager.getInstance().attach(tilesManager);
                SyncManager.getInstance().startSynchronization();
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

    private void slideDown(){
        View generalPanel = findViewById(R.id.general_panel);
        ImageView slideButton = (ImageView)this.findViewById(R.id.slide_button);
        int distanceY = generalPanel.getHeight() - slideButton.getHeight();
        generalPanel.animate().translationY(distanceY);
        slideButton.setImageResource(R.drawable.slide_up_icon);
    }

    private void slideUp(){
        View generalPanel = findViewById(R.id.general_panel);
        generalPanel.animate().translationY(0);
        ImageView slideButton = (ImageView)this.findViewById(R.id.slide_button);
        slideButton.setImageResource(R.drawable.slide_down_icon);
    }
}
