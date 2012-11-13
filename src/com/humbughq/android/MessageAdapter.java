package com.humbughq.android;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MessageAdapter extends ArrayAdapter<Message> {

    public MessageAdapter(Context context, List<Message> objects) {
        super(context, 0, objects);
    }

    public View getView(int position, View convertView, ViewGroup ignored) {
        LinearLayout tile;

        Context context = this.getContext();
        Message message = getItem(position);

        if (convertView != null) {
            tile = (LinearLayout) convertView;
            tile.removeAllViews();
        } else {
            tile = new LinearLayout(context);
        }

        tile.setOrientation(LinearLayout.VERTICAL);

        LinearLayout envelopeTile = new LinearLayout(context);
        envelopeTile.setOrientation(LinearLayout.HORIZONTAL);
        if (message.getType() == Message.STREAM_MESSAGE) {
            envelopeTile.setBackgroundResource(R.drawable.stream_header);
        } else {
            envelopeTile.setBackgroundResource(R.drawable.huddle_header);
        }

        tile.addView(envelopeTile);

        TextView display_recipient = new TextView(context);
        if (message.getType() != Message.STREAM_MESSAGE) {
            display_recipient.setText(context.getString(R.string.huddle_text)
                    + " " + message.getRecipient());
            display_recipient.setTextColor(Color.WHITE);
        } else {
            display_recipient.setText(message.getRecipient());
        }
        display_recipient.setTypeface(Typeface.DEFAULT_BOLD);
        display_recipient.setGravity(Gravity.CENTER_HORIZONTAL);
        display_recipient.setPadding(10, 5, 10, 5);

        envelopeTile.addView(display_recipient);

        if (message.getType() == Message.STREAM_MESSAGE) {
            TextView sep = new TextView(context);
            sep.setText(" | ");

            TextView instance = new TextView(context);
            instance.setText(message.getSubject());
            instance.setPadding(10, 5, 10, 5);

            envelopeTile.addView(sep);
            envelopeTile.addView(instance);
        }

        TextView senderName = new TextView(context);

        senderName.setText(message.getSender());
        senderName.setPadding(10, 5, 10, 5);
        senderName.setTypeface(Typeface.DEFAULT_BOLD);

        tile.addView(senderName);

        TextView contentView = new TextView(context);
        String content = message.getContent();
        contentView.setText(content);
        contentView.setPadding(10, 0, 10, 10);

        int color = Color.WHITE;

        if (message.getType() != Message.STREAM_MESSAGE) {
            color = context.getResources().getColor(R.color.huddle_body);
        } else {
            color = context.getResources().getColor(R.color.stream_body);
        }

        senderName.setBackgroundColor(color);
        contentView.setBackgroundColor(color);

        tile.addView(contentView);
        tile.setTag(R.id.messageID, message.getID());

        return tile;
    }

    public long getItemId(int position) {
        return this.getItem(position).getID();
    }

}
