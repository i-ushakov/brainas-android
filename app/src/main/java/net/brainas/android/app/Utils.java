package net.brainas.android.app;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.brainas.android.app.domain.models.Condition;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by innok on 12/28/2015.
 */
public class Utils {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);


    public static int generateViewId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Utils.generateViewIdForOldVersions();
        } else {
            return View.generateViewId();
        }
    }

    /**
     * Generate a value suitable for use in {@link \#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    private static int generateViewIdForOldVersions() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getDateTimeGMT() {
        Date date = new Date();
        SimpleDateFormat writeDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        writeDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        return writeDate.format(date);
    }

    public static String printFileToString(File file) throws IOException {
        String contents = Files.toString(file, Charsets.UTF_8);
        return contents;
    }

    /*
     * If date1 > date2 => 1
     * date1 = date2 => 0
     * date1 < date2 => -1
     */
    public static int compareTwoDates(String dateStr1, String dateStr2) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date1 = null;
        Date date2 = null;
        try {
            date1 = formatter.parse(dateStr1);
            date2 = formatter.parse(dateStr2);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date1.compareTo(date2);
    }

    public static ViewParent findParentRecursively(View view, int targetId) {
        if (view.getId() == targetId) {
            return (ViewParent)view;
        }
        View parent = (View) view.getParent();
        if (parent == null) {
            return null;
        }
        return findParentRecursively(parent, targetId);
    }

    public static int dpsToPxs(Integer dps, Context context) {
        Integer pxs;
        Resources r = context.getResources();
        pxs = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps, r.getDisplayMetrics());
        return pxs;
    }

    public static ImageView createImageView(int src, int dps, Context context) {
        ImageView eventTypeImage;
        eventTypeImage = new ImageView(context);
        eventTypeImage.setImageResource(src);
        eventTypeImage.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        eventTypeImage.getLayoutParams().width = Utils.dpsToPxs(100, context);
        eventTypeImage.getLayoutParams().height = Utils.dpsToPxs(100, context);
        return eventTypeImage;
    }
}
