package net.brainas.android.app.UI.views.taskedit;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.brainas.android.app.R;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.models.Event;

/**
 * Created by innok on 3/1/2016.
 */
public class EventRowEdit extends LinearLayout {
    private Context context;

    public EventRowEdit(Context context, Event event) {
        this(context, null, event);
    }

    public EventRowEdit(Context context, AttributeSet attrs, Event event) {
        super(context, attrs);

        this.context = context;
        inflate(getContext(), R.layout.view_event_row_edit, this);

        TextView typeAndParamsView = (TextView)findViewById(R.id.eventTypeAndParams);
        typeAndParamsView.setText(TaskHelper.getEventInfo(event));

        //TextView eventTParamsView = (TextView)findViewById(R.id.eventParams);
        //eventTParamsView.setText(event.getJSONStringWithParams());

        /*ImageView eventIconView = (ImageView)findViewById(R.id.event_icon);
        eventIconView.setImageResource(event.getIconDrawableId());

        TextView eventTypeView = (TextView)findViewById(R.id.event_type);
        eventTypeView.setText(event.getType().toString() + ": ");

        TextView eventTParamsView = (TextView)findViewById(R.id.event_params);
        eventTParamsView.setText(event.getJSONStringWithParams());*/
    }
}
