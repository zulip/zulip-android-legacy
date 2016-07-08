package com.zulip.android.viewholders;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.zulip.android.activities.RecyclerMessageAdapter;

public class HeaderSpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int verticalMargin;

    public HeaderSpaceItemDecoration(int verticalMargin) {
        this.verticalMargin = verticalMargin;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {

        int position = parent.getChildAdapterPosition(view);

        int viewType = parent.getAdapter().getItemViewType(position);
        if (viewType == RecyclerMessageAdapter.VIEWTYPE_MESSAGE_HEADER && position != 0) {
            outRect.top = verticalMargin;
        }
    }
}
