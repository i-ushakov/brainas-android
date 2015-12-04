package net.brainas.android.app.UI.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
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
    public TaskTileView(Context context) {
        super(context);
        init();
    }

    public TaskTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TaskTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_task_tile, this);
        //this.header = (TextView)findViewById(R.id.header);
        //this.description = (TextView)findViewById(R.id.description);
        //this.thumbnail = (ImageView)findViewById(R.id.thumbnail);
        //this.icon = (ImageView)findViewById(R.id.icon);
    }
}

/**
 * Created by Kit Ushakov on 11/8/2015.
 */
/*public class TaskTileView extends View {
    private Task task;

    public TaskTileView(Context context) {
        super(context, null);
       // this(context, null);
    }

    public TaskTileView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        //LayoutInflater inflater = LayoutInflater.from(context);

        //LayoutInflater inflater = (LayoutInflater) context
                //.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //inflater.inflate(R.layout.view_task_tile, this, true);
        //LayoutInflater inflater = (LayoutInflater) context
                //.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //inflater.inflate(R.layout.view_task_tile, this, true);
        //LayoutInflater.from(context).inflate(R.layout.view_task_tile, this, true);

        /*this.task = task;
        final TaskTileView taskTile = this;
        //taskTile.post(new Runnable() {
           // @Override
            //public void run() {
                if (taskTile.task.haveImage()) {
                    try {
                        setTaskImage(taskTile);
                    } catch (BrainasAppException e) {
                        // e.getMessage()
                        e.printStackTrace();
                        setMessageText();
                    }
                } else {
                    setMessageText();
                }
            //}
       // });*/
    /*}

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.task = task;
        /*final TaskTileView taskTile = this;
        if (taskTile.task.haveImage()) {
            try {
                setTaskImage(taskTile);
            } catch (BrainasAppException e) {
                // e.getMessage()
                e.printStackTrace();
                setMessageText();
            }
        } else {
            setMessageText();
        }*/
   /* }

    private int getTextSize() {
        int textSize = (int)((double) this.getWidth()/20); // 35 - coun of symbol/4
        return textSize;
    }

    /*private void setTaskImage(TaskTileView taskTile) throws BrainasAppException {
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
    }*/

    /*private void setMessageText() {
        ImageView taskImageView = (ImageView)this.findViewById(R.id.taskImage);
        TextView taskMessageView = (TextView)this.findViewById(R.id.taskMessage);
        ((LinearLayout) (taskMessageView.getParent())).setGravity(Gravity.CENTER);
        taskMessageView.setGravity(Gravity.CENTER);
        taskMessageView.setTextSize(this.getTextSize());
        taskMessageView.setText(this.task.getMessage());
        ViewGroup vg = ((ViewGroup)taskMessageView.getParent());
        vg.removeView(taskImageView);
        vg.removeView(taskMessageView);
        vg.addView(taskMessageView);
        vg.invalidate();
    }*/


  /*  @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }*/
/*}*/
