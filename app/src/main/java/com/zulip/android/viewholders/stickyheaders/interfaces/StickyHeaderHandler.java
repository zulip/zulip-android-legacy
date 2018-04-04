package com.zulip.android.viewholders.stickyheaders.interfaces;

import android.support.v7.widget.RecyclerView;

import java.util.List;

public interface StickyHeaderHandler {

    /**
     * @return The data set supplied to the {@link RecyclerView.Adapter}
     */
    List<?> getAdapterData();

    /**
     * Updated headerView when it is attached or detached
     * @param headerViewAdapterPosition position of view that has to be updated
     */
    void setAttachedHeader(int headerViewAdapterPosition);
}
