package com.zulip.android.util;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;

public class AnimationHelper {

    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();

    @SuppressLint("NewApi")
    public static void showView(final View view, int duration) {
        ViewPropertyAnimator slideOutAnimator = view.animate()
                .translationX(view.getWidth())
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(duration);

        slideOutAnimator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        slideOutAnimator.start();
    }

    @SuppressLint("NewApi")
    public static void hideView(final View view, int duration) {
        ViewPropertyAnimator slideOutAnimator = view.animate()
                .translationX(0)
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(duration);

        slideOutAnimator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        slideOutAnimator.start();
    }

    @SuppressLint("NewApi")
    public static void hideViewX(final View view, boolean animToRight) {
        ViewPropertyAnimator animator = view.animate()
                .translationX((animToRight) ? view.getWidth() : view.getWidth() * -1)
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(200);

        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.GONE);
                view.setX(0);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.start();
    }

}
