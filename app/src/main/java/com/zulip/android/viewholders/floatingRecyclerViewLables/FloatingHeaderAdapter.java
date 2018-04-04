package com.zulip.android.viewholders.floatingRecyclerViewLables;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * The adapter to assist the {@link FloatingHeaderDecoration} in creating and binding the header views.
 *
 * @param <T> the header view holder
 *            This file has been modified from
 *            https://github.com/edubarr/header-decor/blob/master/lib/src/main/java/ca/barrenechea/widget/recyclerview/decoration/StickyHeaderAdapter.java
 */
public interface FloatingHeaderAdapter<T extends RecyclerView.ViewHolder> {

    /**
     * Returns the header id for the item at the given position.
     *
     * @param position the item position
     * @return the header id
     */
    long getHeaderId(int position);

    /**
     * Creates a new header ViewHolder.
     *
     * @param parent the header's view parent
     * @return a view holder for the created view
     */
    T onCreateHeaderViewHolder(ViewGroup parent);

    /**
     * Updates the header view to reflect the header data for the given position
     *
     * @param viewholder the header view holder
     * @param position   the header's item position
     */
    void onBindHeaderViewHolder(T viewholder, int position);
}
