package net.brainas.android.app.UI.views.taskedit;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
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

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.event_row_layout);
        linearLayout.setBackgroundColor(ContextCompat.getColor(context, event.getBackgroundColor()));
        ((TextView)linearLayout.findViewById(R.id.eventTypeAndParams)).setTextColor(ContextCompat.getColor(context, event.getTextColor()));

        TextView typeAndParamsView = (TextView)findViewById(R.id.eventTypeAndParams);
        typeAndParamsView.setText(((BrainasApp)(BrainasApp.getAppContext())).getTaskHelper().getEventInfo(event));
    }
}
