package com.zulip.android.filters;

import com.zulip.android.models.Message;

/**
 * Listener for narrow actions
 */
public interface NarrowListener {
    void onNarrow(NarrowFilter narrowFilter);
    void onNarrowFillSendBox(Message message);
}
