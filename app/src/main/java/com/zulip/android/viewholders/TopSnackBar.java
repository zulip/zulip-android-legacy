package com.zulip.android.viewholders;


import android.content.Context;
import android.os.CountDownTimer;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.activities.RecyclerMessageAdapter;
import com.zulip.android.util.RemoveViewsOnScroll;

/**
 * Custom view for In App Notification
 * TopSnackBar
 */

public class TopSnackBar {

    private Context context;
    public static final int LENGTH_SHORT = 2000; //2 seconds
    public static final int LENGTH_LONG = 4000; //4 seconds
    public static final int LENGTH_INDEFINITE = -1; //shows until it manually dismissed
    private LinearLayoutManager linearLayoutManager;
    private RecyclerMessageAdapter adapter;
    private TextView tvText;
    private Button showButton;
    private CountDownTimer countDownTimer;

    private boolean isShown = false;

    public LinearLayout getLinearLayout() {
        return linearLayout;
    }

    private LinearLayout linearLayout;

    public TopSnackBar(Context context) {
        linearLayout = new LinearLayout(context);
        this.context = context;
        make();
    }

    /**
     * make snackBar
     */
    public void make() {
        createAndAddLayout();
    }

    /**
     * Show snackBar
     * @param topMargin margin of top
     * @param message to be shown in snackBar
     * @param duration auto dismiss duration
     */
    public void show(int topMargin,String message ,int duration) {
        setText(message);
        if (isShown()) {
            countDownTimer.cancel();
            startTimer(duration);
            return;
        }
        linearLayout.setY(topMargin);
        linearLayout.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(context,
                R.anim.slide_down);
        linearLayout.startAnimation(animation);
        setLayoutBehaviour();
        setShown(true);
        startTimer(duration);
    }

    /**
     * start timer to dismiss after duration
     * @param duration time after which it is automatically dismissed
     */
    private void startTimer(int duration) {
        if (duration != LENGTH_INDEFINITE) {
            countDownTimer = new CountDownTimer(duration, duration) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    if (isShown()) {
                        dismiss();
                    }
                }
            };

            countDownTimer.start();
        }
    }

    /**
     * Dismiss snackBar
     */
    public void dismiss() {
        Animation slideUpAnimation = AnimationUtils.loadAnimation(context,
                R.anim.slide_up);
        slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                linearLayout.setVisibility(View.GONE);
                setShown(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        linearLayout.startAnimation(slideUpAnimation);
        removeLayoutBehaviour();
        setShown(false);
    }

    /**
     * Remove Layout Behaviour
     */
    private void removeLayoutBehaviour() {
        getLLLayoutParams().setBehavior(null);
    }

    /**
     * Create And Add Layout
     */
    private void createAndAddLayout() {
        CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(1f);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams tvLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLayoutParams.weight = 0.7f;

        LinearLayout.LayoutParams showButtonLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        showButtonLayoutParams.weight = 0.3f;

        tvText = new TextView(context);
        tvText.setLayoutParams(tvLayoutParams);
        tvText.setTextColor(ContextCompat.getColor(context, R.color.top_snackbar_text_color));
        tvText.setPadding(24,0,0,0);
        linearLayout.addView(tvText);

        showButton = new Button(context);
        showButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        showButton.setTextColor(ContextCompat.getColor(context, R.color.top_snackbar_show_button_text_color));
        showButton.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        showButton.setLayoutParams(showButtonLayoutParams);
        linearLayout.addView(showButton);

        linearLayout.setVisibility(View.GONE);
        linearLayout.setBackgroundResource(R.drawable.top_snackbar_bg);
    }

    /**
     * Get status of snackBar
     * @return whether snackBar is in view or not currently
     */
    public boolean isShown() {
        return isShown;
    }

    /**
     * Set status of snackBar
     * @param shown is true if snackBar is in view currently
     */
    private void setShown(boolean shown) {
        isShown = shown;
    }

    /**
     * Set LayoutBehaviour so that on scroll it can toggle
     */
    public void setLayoutBehaviour() {
        getLLLayoutParams().setBehavior(new RemoveViewsOnScroll(linearLayoutManager, adapter));
    }

    /**
     * Fet LayoutParams of LinearLayout
     * @return LayoutParams
     */
    private CoordinatorLayout.LayoutParams getLLLayoutParams() {
        return (CoordinatorLayout.LayoutParams) linearLayout.getLayoutParams();
    }

    /**
     * Set LinearLayoutManager
     * @param linearLayoutManager LinearLayoutManager
     */
    public void setMessagesLayoutManager(LinearLayoutManager linearLayoutManager) {
        this.linearLayoutManager = linearLayoutManager;
    }


    /**
     * Set Adapter
     * @param adapter RecyclerMessageAdapter
     */
    public void setMessagesAdapter(RecyclerMessageAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Set Message to SnackBar
     * @param quantityString message
     */
    public void setText(String quantityString) {
        tvText.setText(quantityString);
    }

    /**
     * Set Actions button attributes
     * @param string button text
     * @param onClickListener of button
     */
    public void setAction(int string, View.OnClickListener onClickListener) {
        showButton.setText(context.getString(string));
        showButton.setOnClickListener(onClickListener);
    }
}
