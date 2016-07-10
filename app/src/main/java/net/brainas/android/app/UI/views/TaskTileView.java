package net.brainas.android.app.UI.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.brainas.android.app.BrainasAppException;
import net.brainas.android.app.R;
import net.brainas.android.app.activities.TaskCardActivity;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

public class TaskTileView extends RelativeLayout implements View.OnClickListener{
    private Context context;
    private boolean isContentSet = false;
    private Task task;
    private ImageView taskImageView;
    private TextView taskMessageView;

    public TaskTileView(Context context, Task task) {
        super(context);
        init(task, context);
    }

    public TaskTileView(Context context, AttributeSet attrs, Task task) {
        super(context, attrs);
        this.context = context;
        init(task, context);
    }

    public TaskTileView(Context context, AttributeSet attrs, int defStyle, Task task) {
        super(context, attrs, defStyle);
        this.context = context;
        init(task, context);
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


    private void init(Task task, Context context) {
        this.task = task;
        this.context = context;
        inflate(getContext(), R.layout.view_task_tile, this);
        this.setOnClickListener(this);
    }

    private void setContent() {
        taskImageView = (ImageView)this.findViewById(R.id.taskImage);
        taskMessageView = (TextView)this.findViewById(R.id.taskMessage);

        taskMessageView.post(new Runnable() {
            @Override
            public void run() {
                if (task.haveImage()) {
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
            }
        });

        isContentSet = true;
    }

    private void setTaskImage() throws BrainasAppException {
        int imageSize = (int)((double)this.getWidth()/1.5);
        int marginTop = (int)((float)imageSize/4);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(imageSize,imageSize);
        layoutParams.setMargins(0, marginTop, 0, 0);
        //layoutParams.gravity= Gravity.CENTER;
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        taskImageView.setLayoutParams(layoutParams);
        taskImageView.setImageBitmap(this.task.getPicture().getBitmap());
        ((ViewGroup)taskMessageView.getParent()).removeView(taskMessageView);
    }

    private void setMessageText() {
        ViewGroup vg = ((ViewGroup)taskImageView.getParent());
        vg.removeView(taskImageView);
        taskMessageView.setGravity(Gravity.CENTER);
        //taskMessageView.setTextSize(this.getTextSize());
        taskMessageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.getTextSize());
        taskMessageView.setText(this.task.getMessage());
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)taskMessageView.getLayoutParams();
        params.setMargins(8, 8, 8, 8);
        taskMessageView.setLayoutParams(params);
    }

    private int getTextSize() {
        int textSize = (int)((double) this.getWidth()/7); // 35 - coun of symbol/4
        return textSize;
    }

    @Override
    public void onClick(View v) {
        Intent taskCardIntent = new Intent(context, TaskCardActivity.class);
        Bundle b = new Bundle();
        b.putLong("taskId", task.getId());
        taskCardIntent.putExtras(b);
        context.startActivity(taskCardIntent);
    }
}
