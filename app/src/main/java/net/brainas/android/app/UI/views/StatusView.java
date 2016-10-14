package net.brainas.android.app.UI.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.brainas.android.app.R;
import net.brainas.android.app.domain.models.Task;

/**
 * Created by kit on 10/12/2016.
 */
public class StatusView extends LinearLayout {
    View rootView;
    TextView statusTitle;
    Task.STATUSES status;
    Context context;

    public StatusView(Context context, Task.STATUSES status) {
        this(context, null, status);
    }

    public StatusView(Context context, AttributeSet attrs, Task.STATUSES status) {
        super(context, attrs);
        this.context = context;
        if (status != null) {
            this.status = status;
        } else if (attrs != null){
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.StatusView, 0, 0);
            String statusValue = a.getString(R.styleable.StatusView_statusValue);
            a.recycle();

            //switch (statusValue) {
            // TODO to get status fron xml layouts
            //}
        } else {
            return;
        }
        init(context);
    }

    private void init(Context context) {
        rootView = inflate(context, R.layout.view_status, this);
        statusTitle = (TextView) rootView.findViewById(R.id.statusTitle);
        statusTitle.setText(getShortStatusLable(status));
        statusTitle.setTextColor(getTextColor(status));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rootView.setBackground(getBackground(status));
        } else {
            rootView.setBackgroundDrawable(getBackground(status));
        }
    }

    private String getShortStatusLable(Task.STATUSES status) {
        switch (status.name()) {
            case "ACTIVE" :
                return "A";
            case "WAITING" :
                return "W";
            case "TODO" :
                return "T";
            case "DONE" :
                return "D";
            case "CANCELED" :
                return "C";
            case "DISABLED" :
                return "Dis";
        }
        return  null;
    }

    private Drawable getBackground(Task.STATUSES status) {
        switch (status.name()) {
            case "ACTIVE" :
                return ContextCompat.getDrawable(context, R.drawable.active_status_rc_shape);
            case "WAITING" :
                return ContextCompat.getDrawable(context, R.drawable.waitning_status_rc_shape);
            case "TODO" :
                return ContextCompat.getDrawable(context, R.drawable.todo_status_rc_shape);
            case "DONE" :
                return ContextCompat.getDrawable(context, R.drawable.closed_status_rc_shape);
            case "CANCELED" :
                return ContextCompat.getDrawable(context, R.drawable.closed_status_rc_shape);
            case "DISABLED" :
                return ContextCompat.getDrawable(context, R.drawable.closed_status_rc_shape);
            default :
        }

        return ContextCompat.getDrawable(context, R.drawable.active_status_rc_shape);
    }

    private int getTextColor(Task.STATUSES status) {
        switch (status.name()) {
            case "ACTIVE" :
                return ContextCompat.getColor(context, R.color.white);
                //return getResources().getColor(R.color.white);
            case "WAITING" :
                return ContextCompat.getColor(context, R.color.white);
            case "TODO" :
                return ContextCompat.getColor(context, R.color.white);
            case "DONE" :
                return ContextCompat.getColor(context, R.color.black);
            case "CANCELED" :
                return ContextCompat.getColor(context, R.color.black);
            case "DISABLED" :
                return ContextCompat.getColor(context, R.color.black);
            default :
                return ContextCompat.getColor(context, R.color.white);
        }
    }
}
