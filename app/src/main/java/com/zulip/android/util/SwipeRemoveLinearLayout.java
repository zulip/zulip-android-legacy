package com.zulip.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class SwipeRemoveLinearLayout extends LinearLayout {

    private float x1, x2;
    private static int MIN_DISTANCE = 150;
    private leftToRightSwipeListener leftToRightSwipeListen;

    public SwipeRemoveLinearLayout(Context context) {
        super(context);
        setupMinimumDistance();
    }

    public SwipeRemoveLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupMinimumDistance();
    }

    public SwipeRemoveLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupMinimumDistance();
    }

    private void setupMinimumDistance() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        MIN_DISTANCE = metrics.widthPixels / 8;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = ev.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    leftToRightSwipeListen.removeChatBox((deltaX > 0));
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public interface leftToRightSwipeListener {
        void removeChatBox(boolean animToRight);
    }

    public void registerToSwipeEvents(leftToRightSwipeListener listener) {
        this.leftToRightSwipeListen = listener;
    }
}
