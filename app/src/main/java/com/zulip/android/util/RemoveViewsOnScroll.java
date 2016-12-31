package com.zulip.android.util;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import com.zulip.android.R;

/**
 * This hides the {@link AppBarLayout} and {@link android.support.design.widget.FloatingActionButton} when the
 * recyclerView is scrolled, used in here {@link com.zulip.android.R.layout#main} as a behaviour.
 */
public class RemoveViewsOnScroll extends CoordinatorLayout.Behavior<View> {
    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
    private int changeInYDir;
    private boolean mIsShowing;
    private boolean isViewHidden;
    private static float toolbarHeight;
    private View chatBox;

    public RemoveViewsOnScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            toolbarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @SuppressLint("NewApi")
    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && changeInYDir < 0 || dy < 0 && changeInYDir > 0) {
            child.animate().cancel();
            changeInYDir = 0;
        }

        changeInYDir += dy;
        if (changeInYDir > toolbarHeight && child.getVisibility() == View.VISIBLE && !isViewHidden)
            hideView(child);
        else if (changeInYDir < 0 && child.getVisibility() == View.GONE && !mIsShowing) {
            if (child instanceof FloatingActionButton) {
                if (chatBox == null)
                    chatBox = coordinatorLayout.findViewById(R.id.messageBoxContainer);
                if (chatBox.getVisibility() == View.VISIBLE) {
                    return;
                }
            }
            showView(child);
        }

    }

    @SuppressLint("NewApi")
    private void hideView(final View view) {
        isViewHidden = true;
        ViewPropertyAnimator animator = view.animate()
                .translationY((view instanceof AppBarLayout) ? -1 * view.getHeight() : view.getHeight())
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(200);

        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isViewHidden = false;
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isViewHidden = false;
                if (!mIsShowing)
                    showView(view);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.start();
    }

    @SuppressLint("NewApi")
    private void showView(final View view) {
        mIsShowing = true;
        ViewPropertyAnimator animator = view.animate()
                .translationY(0)
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(200);

        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mIsShowing = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mIsShowing = false;
                if (!isViewHidden)
                    hideView(view);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.start();
    }
}
