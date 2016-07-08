package com.zulip.android.activities;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RecyclerMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEWTYPE_MESSAGE_HEADER = 1;
    private static final int VIEWTYPE_MESSAGE = 2;
    private static final int VIEWTYPE_HEADER = 3; //At position 0
    private static final int VIEWTYPE_FOOTER = 4; //At end position

    private static String privateHuddleText;
    private List<Object> items;
    private ZulipApp zulipApp;
    private Context context;
    private NarrowListener narrowListener;
    private
    @ColorInt
    int mDefaultStreamHeaderColor;

    @ColorInt
    private int mDefaultPrivateMessageColor;

    RecyclerMessageAdapter(List<Message> messageList, final Context context, boolean startedFromFilter) {
        super();
        items = new ArrayList<>();
        zulipApp = ZulipApp.get();
        this.context = context;
        narrowListener = (NarrowListener) context;
        mDefaultStreamHeaderColor = ContextCompat.getColor(context, R.color.stream_header);
        mDefaultPrivateMessageColor = ContextCompat.getColor(context, R.color.huddle_body);
        privateHuddleText = context.getResources().getString(R.string.huddle_text);
    }
}
