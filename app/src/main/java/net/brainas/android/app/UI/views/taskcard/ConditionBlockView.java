package net.brainas.android.app.UI.views.taskcard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.shapes.Shape;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import net.brainas.android.app.R;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.fragments.MapFragment;

import java.util.ArrayList;

/**
 * Created by innok on 12/11/2015.
 */
public class ConditionBlockView extends LinearLayout {
    private Condition condition;
    private int containerId = 0;

    public ConditionBlockView(Context context, Condition condition) {
        this(context, null, condition);
    }

    public ConditionBlockView(Context context, AttributeSet attrs, Condition condition) {
        super(context, attrs);

        this.condition = condition;
        inflate(getContext(), R.layout.view_condition_block, this);

        ViewGroup eventsRowViewGroup = (ViewGroup) findViewById(R.id.condition_events_row);
        //containerId = Utils.generateViewId();
        //eventsRowViewGroup.setId(containerId);
        ArrayList<Event> events = condition.getEvents();
        for (Event event : events) {
            LinearLayout eventView = new EventRowView(context, event);
            ((TextView)eventView.findViewById(R.id.event_type)).setTextColor(ContextCompat.getColor(context, event.getTextColor()));
            ((TextView)eventView.findViewById(R.id.event_params)).setTextColor(ContextCompat.getColor(context, event.getTextColor()));
            eventsRowViewGroup.addView(eventView);
        }

        final ViewGroup conditionDetailsViewGroup = (ViewGroup) findViewById(R.id.condition_details);
        conditionDetailsViewGroup.setVisibility(View.GONE);
        Event event = condition.getEvents().get(0);
        if (event.getType().equals(Event.TYPES.GPS)) {
            eventsRowViewGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (conditionDetailsViewGroup.getVisibility() == View.VISIBLE) {
                        collapseConditionDetails(conditionDetailsViewGroup);
                    } else {
                        expandConditionsDetails(conditionDetailsViewGroup);
                    }
                }
            });
        }

        ((GradientDrawable)findViewById(R.id.condition_block).getBackground()).setColor(ContextCompat.getColor(context, event.getBackgroundColor()));
        //findViewById(R.id.condition_block).setBackgroundColor(ContextCompat.getColor(context, event.getBackgroundColor()));

        setConditionDetails((AppCompatActivity) context, condition);
    }

    public static void expandConditionsDetails(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapseConditionDetails(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private void setConditionDetails(AppCompatActivity context,Condition condition) {
        LatLng gpsCoordinates = getGPSCoordinates(condition.getEvents());
        if (gpsCoordinates != null) {
            MapFragment.newInstance(
                    context,
                    getResources().getIdentifier("mapContainer1", "id", context.getPackageName()),
                    gpsCoordinates
            );
        }
    }

    LatLng getGPSCoordinates(ArrayList<Event> events) {
        LatLng latLng = null;
        for (Event event : events) {
            if (event instanceof EventLocation) {
                double lat = ((EventLocation)event).getLat();
                double lng = ((EventLocation)event).getLng();
                latLng = new LatLng(lat, lng);
            }
        }
        return latLng;
    }
}