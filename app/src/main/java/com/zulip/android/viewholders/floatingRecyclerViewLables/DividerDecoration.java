package com.zulip.android.viewholders.floatingRecyclerViewLables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

/**
 * A simple divider decoration.
 * For People Drawer RecyclerView
 * This file has been modified from
 * https://github.com/edubarr/header-decor/blob/master/lib/src/main/java/ca/barrenechea/widget/recyclerview/decoration/DividerDecoration.java
 */
public class DividerDecoration extends RecyclerView.ItemDecoration {

    private int mHeight;

    private DividerDecoration(int height) {
        mHeight = height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, mHeight);
    }

    /**
     * A basic builder for divider decorations. The default builder creates a 1px thick black divider decoration.
     */
    public static class Builder {
        private int mHeight;

        public Builder(Context context) {
            mHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 1f, context.getResources().getDisplayMetrics());
        }

        /**
         * Instantiates a DividerDecoration with the specified parameters.
         *
         * @return a properly initialized DividerDecoration instance
         */
        public DividerDecoration build() {
            return new DividerDecoration(mHeight);
        }
    }
}
