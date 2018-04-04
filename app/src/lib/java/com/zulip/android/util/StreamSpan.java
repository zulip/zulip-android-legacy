package com.zulip.android.util;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import com.zulip.android.ZulipApp;
import com.zulip.android.models.Stream;

import org.apache.commons.lang.StringUtils;

/**
 * Custom ClickableSpan to support #stream-name click. The user is taken to the last message read
 * in the stream.
 */

public class StreamSpan extends ClickableSpan {
    private String streamId;
    private int color;

    public StreamSpan(String streamId, int color) {
        this.streamId = streamId;
        this.color = color;
    }

    /**
     * Performs the click action associated with this span.
     */
    @Override
    public void onClick(View widget) {
        Context context = widget.getContext().getApplicationContext();

        // get stream name from streamId string
        String streamName = null;
        if (StringUtils.isNumeric(streamId)) {
            Stream stream = Stream.getById(ZulipApp.get(), Integer.parseInt(streamId));
            if (stream != null) {
                streamName = stream.getName();
            }

            // go to last message read in the stream
            if (streamName != null) {
                (((ZulipApp) context).getZulipActivity()).doNarrowToLastRead(streamName);
            }
        }
    }

    /**
     * Makes the text underlined and in the link color.
     */
    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(this.color);
        ds.setUnderlineText(false);
    }
}
