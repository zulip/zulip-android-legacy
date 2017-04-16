package com.zulip.android.viewholders.stickyheaders.interfaces;

import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.zulip.android.ZulipApp;

public interface RetrieveHeaderView {

    int DEFAULT_VIEW_TYPE = -1;

    RecyclerView.ViewHolder getViewHolderForPosition(int headerPositionToShow);

    class RecyclerRetrieveHeaderView implements RetrieveHeaderView {

        private final RecyclerView recyclerView;

        private RecyclerView.ViewHolder currentViewHolder;
        private int currentViewType;

        public RecyclerRetrieveHeaderView(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
            this.currentViewType = DEFAULT_VIEW_TYPE;
        }

        @Override
        public RecyclerView.ViewHolder getViewHolderForPosition(int position) {
            if (currentViewType != recyclerView.getAdapter().getItemViewType(position)) {
                currentViewType = recyclerView.getAdapter().getItemViewType(position);
                currentViewHolder = recyclerView.getAdapter().createViewHolder(
                        (ViewGroup) recyclerView.getParent(), currentViewType);
            }
            //set top margin for sticky header to 0 as in message_header.xml it is present which is only required when header is in between of messages
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) currentViewHolder.itemView.getLayoutParams();
            layoutParams.setMargins(0, ZulipApp.get().getZulipActivity().getFloatingHeaderTopMargin(), 0, 0);
            return currentViewHolder;
        }
    }
}
