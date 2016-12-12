package net.brainas.android.app.UI.logic;

import android.content.Context;
import android.graphics.Point;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.UI.views.TaskTileView;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.synchronization.SynchronizationManager;
import net.brainas.android.app.infrustructure.UserAccount;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kit Ushakov on 11/8/2015.
 */
public class ReminderScreenManager implements
        SynchronizationManager.TaskSyncObserver,
        ActivationManager.ActivationObserver,
        Task.TaskChangesObserver,
        AccountsManager.SingInObserver {
    private int panelWidth;
    private ViewGroup tilesPanel;
    private Context context;
    private ScrollView rightScrollPanel;
    private int scrollPanelWidth;
    private int scrollPanelHeight;
    private List<ReminderTileCell> tilesGrid = new ArrayList<>();

    private BrainasApp app;
    private TasksManager tasksManager;

    public ReminderScreenManager(ViewGroup tilesPanel) {
        this.tilesPanel = tilesPanel;
        this.context = tilesPanel.getContext();
        this.panelWidth = tilesPanel.getWidth();
        this.tilesGrid = this.calculateTilesGrid(panelWidth);
        tasksManager = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager();
        app = (BrainasApp)BrainasApp.getAppContext();
        app.getActivationManager().attach(this);
        app.getSynchronizationManager().attach(this);
    }

    public void refreshTilesWithActiveTasks() {
        if (app.getAccountsManager().isUserSingIn()) {
            List<Task> activeTasks = app.getTasksManager().getActiveList();
            List<TaskTileView> tiles = this.initTiles(activeTasks);
            this.placeTiles(tiles);
        }
    }

    @Override
    public void updateAfterSync() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTilesWithActiveTasks();
            }
        });
    }

    @Override
    public void updateAfterActivation() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTilesWithActiveTasks();
            }
        });
    }

    @Override
    public void updateAfterTaskWasChanged() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTilesWithActiveTasks();
            }
        });
    }

    public void updateAfterCheckConditions() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTilesWithActiveTasks();
            }
        });
    }

    @Override
    public void updateAfterSingIn(UserAccount userAccount) {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTilesWithActiveTasks();
            }
        });
    }

    @Override
    public void updateAfterSingOut() {
        ((BrainasApp)(BrainasApp.getAppContext())).getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                refreshTilesWithActiveTasks();
            }
        });
    }
;

    private List<TaskTileView> initTiles(List<Task> tasks) {
        if (tasks != null) {
            List<TaskTileView> tiles = new ArrayList<TaskTileView>();
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                tiles.add(new TaskTileView(tilesPanel.getContext(), task));
                task.attachObserver(this);
            }
            return tiles;
        } else {
            return null;
        }
    }

    private void placeTiles(List<TaskTileView> tiles) {
        this.tilesPanel.removeAllViews();
        if (tiles != null) {
            for (int i = 0; i < tiles.size() && i < 10; i++) {
                TaskTileView tile = tiles.get(i);
                ReminderTileCell tc = tilesGrid.get(i);
                int cellSize = tc.getSize();
                Point position = tc.getPosition();
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cellSize, cellSize);
                params.setMargins(position.y, position.x, 0, 0);
                tile.setLayoutParams(params);
                this.tilesPanel.addView(tile);
            }
            addRightScrollPanel();
            if ( tiles.size() > 10) {
                for (int i = 10; i < tiles.size(); i++) {
                    addTilesToRightPanel(tiles.get(i));
                }
            }
        }
    }

    private List<ReminderTileCell> calculateTilesGrid(int panelWidth) {
        int gridStep = panelWidth/15;

        // 6x6
        this.tilesGrid.add(new ReminderTileCell(0, 0, 6*gridStep));
        this.tilesGrid.add(new ReminderTileCell(0, 6*gridStep, 6*gridStep));
        this.tilesGrid.add(new ReminderTileCell(6*gridStep,0,6*gridStep));
        this.tilesGrid.add(new ReminderTileCell(6*gridStep,6*gridStep,6*gridStep));

        // 4x4
        this.tilesGrid.add(new ReminderTileCell(12*gridStep,0,4*gridStep));
        this.tilesGrid.add(new ReminderTileCell(12*gridStep,4*gridStep,4*gridStep));
        this.tilesGrid.add(new ReminderTileCell(12*gridStep,8*gridStep,4*gridStep));
        this.tilesGrid.add(new ReminderTileCell(16*gridStep,0,4*gridStep));
        this.tilesGrid.add(new ReminderTileCell(16*gridStep,4*gridStep,4*gridStep));
        this.tilesGrid.add(new ReminderTileCell(16*gridStep,8*gridStep,4*gridStep));


        this.scrollPanelWidth = 3 * gridStep;
        this.scrollPanelHeight = 20 * gridStep;
        return  this.tilesGrid;
    }

    private void addRightScrollPanel() {
        this.tilesPanel.removeView(rightScrollPanel);

        rightScrollPanel = new ScrollView(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(scrollPanelWidth, scrollPanelHeight);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rightScrollPanel.setLayoutParams(layoutParams);

        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        rightScrollPanel.addView(innerLayout);

        this.tilesPanel.addView(rightScrollPanel);
    }

    private void addTilesToRightPanel(TaskTileView tile) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(scrollPanelWidth, scrollPanelWidth);
        tile.setLayoutParams(params);
        ((LinearLayout)rightScrollPanel.getChildAt(0)).addView(tile);
    }
}
