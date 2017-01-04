package com.zulip.android.filters;

import com.zulip.android.models.Message;
import com.zulip.android.models.Person;

/**
 * Listener for narrow actions
 */
public interface NarrowListener {
    void onNarrow(NarrowFilter narrowFilter);

    void onNarrow(NarrowFilter narrowFilter, int messageId);

    void onNarrowFillSendBox(Message message, boolean openSoftKeyboard);

    void onNarrowFillSendBoxStream(String stream, String message, boolean openSoftKeyboard);

    void onNarrowFillSendBoxPrivate(Person person[], boolean openSoftKeyboard);
}
