package net.brainas.android.app.UI;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;

import net.brainas.android.app.R;

/**
 * Created by innok on 3/12/2016.
 */
public class UIHelper {
    static public void addClickEffectToButton(View view, Context context) {
        AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(AnimationUtils.loadAnimation(context, R.anim.press_btn_alpha));
        view.startAnimation(animationSet);
        AnimationSet childrenAnimationSet = new AnimationSet(false);
        childrenAnimationSet.addAnimation(AnimationUtils.loadAnimation(context, R.anim.press_btn_scale));
        for(int i=0; i<((ViewGroup)view).getChildCount(); ++i) {
            View childView = ((ViewGroup)view).getChildAt(i);
            childView.startAnimation(childrenAnimationSet);
        }
    }

    static public boolean preventDoubleClick(Long lastClickTime) {
        if (lastClickTime == null) {
            return false;
        }
        if (SystemClock.elapsedRealtime() - lastClickTime < 1000){
            return true;
        }

        return false;
    }

    static public boolean safetyBtnClick(View view, Context context) {
        if (preventDoubleClick((Long)view.getTag())) {
            return false;
        }
        view.setTag(SystemClock.elapsedRealtime());
        addClickEffectToButton(view, context);
        return true;
    }
}
