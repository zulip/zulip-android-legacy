package com.zulip.android.util;

import com.zulip.android.models.Message;

public interface OnItemClickListener {
    void onItemClick(int viewId, int position);

    Message getMessageAtPosition(int position);

    void setContextItemSelectedPosition(int adapterPosition);
}
