package net.brainas.android.app.UI.logic;

import android.graphics.Point;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.UI.views.TaskTileView;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.SyncManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kit Ushakov on 11/8/2015.
 */
public class TilesManager implements SyncManager.TaskSyncObserver {
    private ViewGroup tilesPanel;
    private List<TileCell> tilesGrid = new ArrayList<>();
    private int panelWidth;

    public TilesManager(ViewGroup tilesPanel) {
        this.tilesPanel = tilesPanel;
        this.panelWidth = tilesPanel.getWidth();
        this.tilesGrid = this.calculateTilesGrid(panelWidth);
    }

    public void addTilesWithTasks() {
        TasksManager tasksManager = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager();
        List<Task> tasks = tasksManager.getTasks();
        List<TaskTileView> tiles = this.initTiles(tasks);
        this.placeTiles(tiles);
    }

    private List<TaskTileView> initTiles(List<Task> tasks) {
        List<TaskTileView> tiles = new ArrayList<TaskTileView>();
        for (int i = 0; i < tasks.size(); i++) {
            tiles.add(new TaskTileView(tilesPanel.getContext(), tasks.get(i)));
        }
        return tiles;
    }

    private void placeTiles(List<TaskTileView> tiles) {
        for (int i = 0; i < tiles.size() && i < 5; i++) {
            TaskTileView tile = tiles.get(i);
            TileCell tc = tilesGrid.get(i);
            int cellSize = tc.getSize();
            Point position = tc.getPosition();
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cellSize,cellSize);
            params.setMargins(position.y, position.x, 0, 0);
            tile.setLayoutParams(params);
            this.tilesPanel.addView(tile);
        }
    }

    private List<TileCell> calculateTilesGrid(int panelWidth) {
        int gridStep = panelWidth/15;

        // 10x10
        this.tilesGrid.add(new TileCell(0,0,10*gridStep));

        // 5x5
        this.tilesGrid.add(new TileCell(10*gridStep,0,5*gridStep));
        this.tilesGrid.add(new TileCell(10*gridStep,5*gridStep,5*gridStep));
        this.tilesGrid.add(new TileCell(15*gridStep,0,5*gridStep));
        this.tilesGrid.add(new TileCell(15*gridStep,5*gridStep,5*gridStep));

        return  this.tilesGrid;
    }

    @Override
    public void update() {
        addTilesWithTasks();
    }
}
