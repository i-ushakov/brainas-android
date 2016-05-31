package net.brainas.android.app.UI.views.taskcard;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import net.brainas.android.app.R;
import net.brainas.android.app.activities.taskedit.EditEventActivity;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;

import java.util.ArrayList;

/**
 * Created by innok on 12/11/2015.
 */
public class ConditionBlockView extends LinearLayout {
    private Condition condition;

    public ConditionBlockView(Context context, Condition condition) {
        this(context, null, condition);
    }

    public ConditionBlockView(final Context context, AttributeSet attrs, final Condition condition) {
        super(context, attrs);

        this.condition = condition;
        inflate(getContext(), R.layout.view_condition_block, this);

        ViewGroup eventsRowViewGroup = (ViewGroup) findViewById(R.id.condition_events_row);
        ArrayList<Event> events = condition.getEvents();
        for (Event event : events) {
            LinearLayout eventView = new EventRowView(context, event);
            ((TextView)eventView.findViewById(R.id.event_type)).setTextColor(ContextCompat.getColor(context, event.getTextColor()));
            ((TextView)eventView.findViewById(R.id.event_params)).setTextColor(ContextCompat.getColor(context, event.getTextColor()));
            eventsRowViewGroup.addView(eventView);
        }

        final Event event;
        if (condition.getEvents().size() > 0) {
            event = condition.getEvents().get(0);
        } else {
            return;
        }

        eventsRowViewGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, EditEventActivity.class);
                intent.putExtra("taskLocalId", condition.getParent().getId());
                intent.putExtra("eventId", event.getId());
                context.startActivity(intent);
            }
        });

        ((GradientDrawable)findViewById(R.id.condition_block).getBackground()).setColor(ContextCompat.getColor(context, event.getBackgroundColor()));
    }
}