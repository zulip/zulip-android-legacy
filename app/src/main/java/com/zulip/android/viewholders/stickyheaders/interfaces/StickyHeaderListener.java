package com.zulip.android.viewholders.stickyheaders.interfaces;

import com.zulip.android.viewholders.stickyheaders.RetrieveHeaderView;

/**
 * A listener that can be set by calling {@link com.zulip.android.viewholders.stickyheaders.StickyLayoutManager#setStickyHeaderListener(StickyHeaderListener)}
 * <p>
 * New instance of the same view is created which float's on the top when list is scrolled
 */
public interface StickyHeaderListener {

    /**
     * Called when a Sticky Header has been attached or rebound.
     *
     * @param adapterPosition The position in the adapter data set that this view represents
     */
    void headerAttached(int adapterPosition);

    /**
     * Called when a Sticky Header has been detached or is about to be re-bound.
     * <p>
     * For performance reasons, if the new Sticky Header that will be replacing the current one is
     * of the same view type, the view is reused. In that case, this call will be immediately followed
     * by a call to {@link StickyHeaderListener#headerAttached(int)}} with the same view instance,
     * but after the view is re-bound with the new adapter data.
     * <p>
     * <b>Important</b><br/>
     * {@code adapterPosition} cannot be guaranteed to be the position in the current adapter
     * data set that this view represents. The data may have changed after this view was bound, but
     * before it was detached.
     * <p>
     * It is also possible for {@code adapterPosition} to be {@link RetrieveHeaderView#DEFAULT_VIEW_TYPE}, though this should be a rare case.
     * So check it before using it
     * <p>
     *
     * @param adapterPosition The position in the adapter data set that the header view was created from when originally bound
     */
    void headerDetached(int adapterPosition);
}
