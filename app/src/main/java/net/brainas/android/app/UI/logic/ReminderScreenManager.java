package net.brainas.android.app.UI.logic;

import android.graphics.Point;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.UI.views.TaskTileView;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.Synchronization;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kit Ushakov on 11/8/2015.
 */
public class ReminderScreenManager implements
        Synchronization.TaskSyncObserver,
        ActivationManager.ActivationObserver,
        Task.TaskChangesObserver {
    private int panelWidth;
    private ViewGroup tilesPanel;
    private List<ReminderTileCell> tilesGrid = new ArrayList<>();
    private TasksManager tasksManager;

    public ReminderScreenManager(ViewGroup tilesPanel) {
        this.tilesPanel = tilesPanel;
        this.panelWidth = tilesPanel.getWidth();
        this.tilesGrid = this.calculateTilesGrid(panelWidth);
        tasksManager = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager();
        Synchronization.getInstance().attach(this);
        ((BrainasApp)BrainasApp.getAppContext()).getActivationManager().attach(this);
    }

    public void refreshTilesWithActiveTasks() {
        if (((BrainasApp)BrainasApp.getAppContext()).getAccountsManager().isUserSingIn()) {
            List<Task> activeTasks = tasksManager.getActiveList();
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
            for (int i = 0; i < tiles.size() && i < 5; i++) {
                TaskTileView tile = tiles.get(i);
                ReminderTileCell tc = tilesGrid.get(i);
                int cellSize = tc.getSize();
                Point position = tc.getPosition();
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cellSize, cellSize);
                params.setMargins(position.y, position.x, 0, 0);
                tile.setLayoutParams(params);
                this.tilesPanel.addView(tile);
            }
        }
    }

    private List<ReminderTileCell> calculateTilesGrid(int panelWidth) {
        int gridStep = panelWidth/15;

        // 10x10
        this.tilesGrid.add(new ReminderTileCell(0,0,10*gridStep));

        // 5x5
        this.tilesGrid.add(new ReminderTileCell(10*gridStep,0,5*gridStep));
        this.tilesGrid.add(new ReminderTileCell(10*gridStep,5*gridStep,5*gridStep));
        this.tilesGrid.add(new ReminderTileCell(15*gridStep,0,5*gridStep));
        this.tilesGrid.add(new ReminderTileCell(15 * gridStep, 5 * gridStep, 5 * gridStep));

        return  this.tilesGrid;
    }
}
