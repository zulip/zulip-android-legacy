package com.humbughq.android;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class PointyScrollView extends ScrollView {

    private int pointerPos;
    public HumbugActivity context;

    public PointyScrollView(Context context) {
        super(context);

        this.context = (HumbugActivity) context;
    }

    public PointyScrollView(Context context, int pointerPos) {
        super(context);

        this.context = (HumbugActivity) context;
        this.pointerPos = pointerPos;
    }

    public PointyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = (HumbugActivity) context;
    }

    public PointyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = (HumbugActivity) context;
    }

    public int getSelectedID() {
        return pointerPos;
    }

    public LinearLayout getSelectedTile() {
        return this.context.messageTiles.get(this.pointerPos);
    }

    public void select(Message message) {
        this.select(message.getID());
    }

    public void select(int messageId) {
        this.pointerPos = messageId;
        this.scrollToSelected();
    }

    public void scrollToSelected() {
        Log.i("pointer", "Pointer moving to " + this.pointerPos);
        LinearLayout tile = this.context.messageTiles.get(this.pointerPos);
        if (tile != null) {
            this.context.mainScroller.scrollTo(0,
                    tile.getTop() + tile.getHeight() / 2
                            - this.context.mainScroller.getHeight() / 2);
        } else {
            Log.e("pointer", "Could not find a tile for " + this.pointerPos);
        }
    }

}
