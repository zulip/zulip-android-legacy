package com.zulip.android.viewholders.stickyheaders;

// modified from https://github.com/bgogetap/StickyHeaders/blob/master/stickyheaders/src/main/java/com/brandongogetap/stickyheaders/StickyLayoutManager.java

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.zulip.android.ZulipApp;
import com.zulip.android.viewholders.MessageHeaderParent;
import com.zulip.android.viewholders.stickyheaders.interfaces.StickyHeaderHandler;
import com.zulip.android.viewholders.stickyheaders.interfaces.StickyHeaderListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StickyLayoutManager extends LinearLayoutManager {

    private GetStickyHeaderPosition positioner;
    private StickyHeaderHandler headerHandler;
    private List<Integer> headerPositions;
    private RetrieveHeaderView viewRetriever;
    private RecyclerView recyclerView;
    @Nullable
    private StickyHeaderListener listener;

    public StickyLayoutManager(Context context, StickyHeaderHandler headerHandler) {
        this(context, VERTICAL, false, headerHandler);
        init(headerHandler);
    }

    private StickyLayoutManager(Context context, int orientation, boolean reverseLayout, StickyHeaderHandler headerHandler) {
        super(context, orientation, reverseLayout);
        init(headerHandler);
    }

    private void init(StickyHeaderHandler stickyHeaderHandler) {
        setStickyHeaderHandler(stickyHeaderHandler);
    }

    private void setStickyHeaderHandler(StickyHeaderHandler headerHandler) {
        if (headerHandler != null) {
            this.headerHandler = headerHandler;
            headerPositions = new ArrayList<>();
        } else {
            throw new NullPointerException("StickyHeaderHandler is null");
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        cacheHeaderPositions();
        positioner.reset(getOrientation(), findFirstVisibleItemPosition());
        positioner.updateHeaderState(
                findFirstVisibleItemPosition(), getVisibleHeaders(), viewRetriever);
    }

    private void cacheHeaderPositions() {
        headerPositions.clear();
        for (int i = 0; i < headerHandler.getAdapterData().size(); i++) {
            if (headerHandler.getAdapterData().get(i) instanceof MessageHeaderParent) {
                headerPositions.add(i);
            }
        }
        positioner.setHeaderPositions(headerPositions);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scroll = super.scrollVerticallyBy(dy, recycler, state);
        if (Math.abs(scroll) > 0) {
            positioner.updateHeaderState(
                    findFirstVisibleItemPosition(), getVisibleHeaders(), viewRetriever);
        }
        return scroll;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scroll = super.scrollHorizontallyBy(dx, recycler, state);
        if (Math.abs(scroll) > 0) {
            positioner.updateHeaderState(
                    findFirstVisibleItemPosition(), getVisibleHeaders(), viewRetriever);
        }
        return scroll;
    }

    private Map<Integer, View> getVisibleHeaders() {
        Map<Integer, View> visibleHeaders = new LinkedHashMap<>();

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int dataPosition = getPosition(view);
            if (headerPositions.contains(dataPosition)) {
                visibleHeaders.put(dataPosition, view);
            }
        }
        return visibleHeaders;
    }

    /**
     * Register a callback to be invoked when a header is attached/re-bound or detached.
     *
     * @param listener The callback that will be invoked, or null to unset.
     */
    public void setStickyHeaderListener(@Nullable StickyHeaderListener listener) {
        this.listener = listener;
        if (positioner != null) {
            positioner.setListener(listener);
        }
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        recyclerView = view;
        viewRetriever = new RetrieveHeaderView(recyclerView);
        positioner = new GetStickyHeaderPosition(recyclerView);
        positioner.setListener(listener);
    }
}
