package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * This class is intercepting the touch events to not allow touch events to be reached to
 * InMobi's primary view. As the Adapter wants AdMob to do the click tracking. If this class is
 * not used then the click will be consumed by InMobi's primary view and publisher will not get
 * click callback from AdMob.
 */
public class ClickInterceptorRelativeLayout extends RelativeLayout {

    public ClickInterceptorRelativeLayout(Context context) {
        super(context);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
