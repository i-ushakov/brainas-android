package net.brainas.android.app.UI.views.taskedit;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.activities.taskedit.EditConditionsActivity;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;

import java.util.ArrayList;

/**
 * Created by Kit Ushakov on 2/28/2016.
 */
public class ConditionEditView extends LinearLayout {
    private EditConditionsActivity editConditionsActivity;
    private Condition condition;

    public ConditionEditView(EditConditionsActivity editConditionsActivity, Condition condition) {
        this(editConditionsActivity, null, condition);
    }

    public ConditionEditView(Context context, AttributeSet attrs, Condition condition) {
        super(context, attrs);

        inflate(getContext(), R.layout.view_condition_edit, this);

        this.condition = condition;
        ViewGroup conditionEventsBlock = (ViewGroup) findViewById(R.id.conditionEventsBlock);
        ViewGroup conditionTopPanelLeft = (ViewGroup) findViewById(R.id.conditionTopPanelLeft);
        ArrayList<Event> events = condition.getEvents();
        for (Event event : events) {
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(event.getIconDrawableId());
            conditionTopPanelLeft.addView(imageView);
            imageView.setLayoutParams(new LayoutParams(
                (int) getResources().getDimension(R.dimen.event_type_icon_edit_width),
                (int) getResources().getDimension(R.dimen.event_type_icon_edit_height)));
            LinearLayout eventView = new EventRowEdit(context, event);
            conditionEventsBlock.addView(eventView);
        }
    }

    public Condition getCondition(){
        return condition;
    }

    public void editCondition(View view) {

    }

    /*
     * We are getting default location for setting initial coordinate on map
     * It can be:
     * 1.Current user location if we know it
     * 2. The location that was set before
     * 3. Maybe location of country's capital or a city that linked with user's current  time zone
     */
    private Pair<Long, Long> getDefaultLocation() {
        return new Pair<Long, Long>(1l,1l);
    }
}
