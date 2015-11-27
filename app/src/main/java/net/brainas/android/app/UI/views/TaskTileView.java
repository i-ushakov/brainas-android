package net.brainas.android.app.UI.views;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.brainas.android.app.BrainasAppException;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

/**
 * Created by Kit Ushakov on 11/8/2015.
 */
public class TaskTileView extends LinearLayout {
    private Task task;

    public TaskTileView(final Context context, Task task) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.view_task_tile, this, true);

        this.task = task;
        final TaskTileView taskTile = this;
        taskTile.post(new Runnable() {
            @Override
            public void run() {
                if (taskTile.task.haveImage()) {
                    try {
                        setTaskImage(taskTile);
                    } catch (BrainasAppException e) {
                        // e.getMessage()
                        e.printStackTrace();
                        setMessageText(taskTile);
                    }
                } else {
                    setMessageText(taskTile);
                }
            }
        });
    }

    private int getTextSize() {
        int textSize = (int)((double) this.getWidth()/20); // 35 - coun of symbol/4
        return textSize;
    }

    private void setTaskImage(TaskTileView taskTile) throws BrainasAppException {
        ImageView taskImageView = (ImageView)taskTile.findViewById(R.id.taskImage);
        TextView taskMessageView = (TextView)taskTile.findViewById(R.id.taskMessage);
        int imageSize = (int)((double)taskTile.getWidth()/1.5);
        int marginTop = (int)((float)imageSize/4);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageSize,imageSize);
        layoutParams.setMargins(0, marginTop, 0, 0);
        layoutParams.gravity= Gravity.CENTER;
        taskImageView.setLayoutParams(layoutParams);
        taskImageView.setImageBitmap(InfrustructureHelper.getTaskImage(taskTile.task));
        ((ViewGroup)taskMessageView.getParent()).removeView(taskMessageView);
    }

    private void setMessageText(TaskTileView taskTile) {
        ImageView taskImageView = (ImageView)taskTile.findViewById(R.id.taskImage);
        TextView taskMessageView = (TextView)taskTile.findViewById(R.id.taskMessage);
        ((LinearLayout) (taskMessageView.getParent())).setGravity(Gravity.CENTER);
        taskMessageView.setGravity(Gravity.CENTER);
        taskMessageView.setTextSize(taskTile.getTextSize());
        taskMessageView.setText(taskTile.task.getMessage());
        ((ViewGroup)taskImageView.getParent()).removeView(taskImageView);
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
