package net.brainas.android.app.UI.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.brainas.android.app.BrainasAppException;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

public class TaskTileView extends RelativeLayout {
    private boolean isContentSet = false;
    private Task task;
    private ImageView taskImageView;
    private TextView taskMessageView;

    public TaskTileView(Context context, Task task) {
        super(context);
        init(task);
    }

    public TaskTileView(Context context, AttributeSet attrs, Task task) {
        super(context, attrs);
        init(task);
    }

    public TaskTileView(Context context, AttributeSet attrs, int defStyle, Task task) {
        super(context, attrs, defStyle);
        init(task);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isContentSet) {
            setContent();
        }
    }


    private void init(Task task) {
        this.task = task;
        inflate(getContext(), R.layout.view_task_tile, this);
    }

    private void setContent() {
        taskImageView = (ImageView)this.findViewById(R.id.taskImage);
        taskMessageView = (TextView)this.findViewById(R.id.taskMessage);

        if (this.task.haveImage()) {
            try {
                setTaskImage();
            } catch (BrainasAppException e) {
                // e.getMessage()
                e.printStackTrace();
                setMessageText();
            }
        } else {
            setMessageText();
        }
        isContentSet = true;
    }

    private void setTaskImage() throws BrainasAppException {
        int imageSize = (int)((double)this.getWidth()/1.5);
        int marginTop = (int)((float)imageSize/4);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageSize,imageSize);
        layoutParams.setMargins(0, marginTop, 0, 0);
        layoutParams.gravity= Gravity.CENTER;
        taskImageView.setLayoutParams(layoutParams);
        taskImageView.setImageBitmap(InfrustructureHelper.getTaskImage(this.task));
        ((ViewGroup)taskMessageView.getParent()).removeView(taskMessageView);
    }

    private void setMessageText() {
        ((RelativeLayout) (taskMessageView.getParent())).setGravity(Gravity.CENTER);
        taskMessageView.setGravity(Gravity.CENTER);
        taskMessageView.setTextSize(this.getTextSize());
        taskMessageView.setText(this.task.getMessage());
        ((ViewGroup)taskImageView.getParent()).removeView(taskImageView);
    }

    private int getTextSize() {
        int textSize = (int)((double) this.getWidth()/20); // 35 - coun of symbol/4
        return textSize;
    }
}
