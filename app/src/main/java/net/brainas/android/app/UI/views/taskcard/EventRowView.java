package net.brainas.android.app.UI.views.taskcard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.models.Event;

/**
 * Created by innok on 12/11/2015.
 */
public class EventRowView extends LinearLayout {
    private Context context;
    private int iconDrawableId;

    public EventRowView(Context context, Event event) {
        this(context, null, event);
    }

    public EventRowView(Context context, AttributeSet attrs, Event event) {
        super(context, attrs);

        this.context = context;
        inflate(getContext(), R.layout.view_event_row, this);

        ImageView eventIconView = (ImageView)findViewById(R.id.event_icon);
        eventIconView.setImageResource(event.getIconDrawableId());

        TextView eventTypeView = (TextView)findViewById(R.id.event_type);
        eventTypeView.setText(event.getType().toString() + ": ");

        TextView eventParamsView = (TextView)findViewById(R.id.event_params);
        TaskHelper taskHelper = ((BrainasApp)BrainasApp.getAppContext()).getTaskHelper();
        eventParamsView.setText(taskHelper.getEventInfo(event));
    }
}
