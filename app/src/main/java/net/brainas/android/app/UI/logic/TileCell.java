package net.brainas.android.app.UI.logic;

import android.graphics.Point;

/**
 * Created by Kit Ushakov on 11/8/2015.
 */
public class TileCell {
    private Point position = new Point();
    private int size = 0;

    TileCell(int positionX, int positionY,  int size) {
        this.position.set(positionX, positionY);
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public Point getPosition() {
        return this.position;
    }

}
