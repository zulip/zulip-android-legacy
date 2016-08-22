package com.zulip.android.util;

import com.zulip.android.models.Message;

/**
 * An interface between the {@link com.zulip.android.viewholders.MessageHolder} and
 * {@link com.zulip.android.viewholders.MessageHeaderParent.MessageHeaderHolder}.
 */
public interface OnItemClickListener {
    void onItemClick(int viewId, int position);

    Message getMessageAtPosition(int position);

    void setContextItemSelectedPosition(int adapterPosition);
}
