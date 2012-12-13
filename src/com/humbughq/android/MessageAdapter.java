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

/**
 * Adapter which stores Messages in a view, and generates LinearLayouts for
 * consumption by the ListView which displays the view.
 */
public class MessageAdapter extends ArrayAdapter<Message> {

    public MessageAdapter(Context context, List<Message> objects) {
        super(context, 0, objects);
    }

    /**
     * Creates a new (empty) message tile with all the relevant fillable Views
     * tagged.
     * 
     * @return the constructed message tile
     */
    private LinearLayout generateTile() {
        Context context = this.getContext();

        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(LinearLayout.VERTICAL);

        LinearLayout envelopeTile = new LinearLayout(context);
        envelopeTile.setOrientation(LinearLayout.HORIZONTAL);
        envelopeTile.setTag("envelope");

        tile.addView(envelopeTile);

        TextView displayRecipient = new TextView(context);
        displayRecipient.setTag("display_recipient");

        displayRecipient.setTypeface(Typeface.DEFAULT_BOLD);
        displayRecipient.setGravity(Gravity.CENTER_HORIZONTAL);
        displayRecipient.setPadding(10, 5, 10, 5);

        envelopeTile.addView(displayRecipient);

        TextView sep = new TextView(context);
        sep.setText(" | ");
        sep.setTag("sep");

        TextView instance = new TextView(context);
        instance.setPadding(10, 5, 10, 5);
        instance.setTag("instance");

        envelopeTile.addView(sep);
        envelopeTile.addView(instance);

        TextView senderName = new TextView(context);

        senderName.setTag("senderName");
        senderName.setPadding(10, 5, 10, 5);
        senderName.setTypeface(Typeface.DEFAULT_BOLD);

        tile.addView(senderName);

        TextView contentView = new TextView(context);
        contentView.setTag("contentView");
        contentView.setPadding(10, 0, 10, 10);

        tile.addView(contentView);

        return tile;
    }

    public View getView(int position, View convertView, ViewGroup ignored) {

        Context context = this.getContext();
        Message message = getItem(position);
        LinearLayout tile;

        if (convertView == null) {
            // We didn't get passed a tile, so construct a new one.
            // In the future, we should inflate from a layout here.
            tile = generateTile();
        } else {
            tile = (LinearLayout) convertView;
        }

        LinearLayout envelopeTile = (LinearLayout) tile
                .findViewWithTag("envelope");
        TextView display_recipient = (TextView) tile
                .findViewWithTag("display_recipient");

        if (message.getType() == MessageType.STREAM_MESSAGE) {
            envelopeTile.setBackgroundResource(R.drawable.stream_header);
        } else {
            envelopeTile.setBackgroundResource(R.drawable.huddle_header);
        }

        if (message.getType() != MessageType.STREAM_MESSAGE) {
            display_recipient.setText(context.getString(R.string.huddle_text)
                    + " " + message.getDisplayRecipient());
            display_recipient.setTextColor(Color.WHITE);
        } else {
            display_recipient.setText(message.getDisplayRecipient());
            display_recipient.setTextColor(Color.BLACK);
        }

        TextView sep = (TextView) tile.findViewWithTag("sep");
        TextView instance = (TextView) tile.findViewWithTag("instance");

        if (message.getType() == MessageType.STREAM_MESSAGE) {
            instance.setVisibility(View.VISIBLE);
            sep.setVisibility(View.VISIBLE);
            instance.setText(message.getSubject());
        } else {
            instance.setVisibility(View.GONE);
            sep.setVisibility(View.GONE);
        }

        TextView senderName = (TextView) tile.findViewWithTag("senderName");
        senderName.setText(message.getSender().getName());

        TextView contentView = (TextView) tile.findViewWithTag("contentView");
        contentView.setText(message.getContent());

        int color;

        if (message.getType() != MessageType.STREAM_MESSAGE) {
            color = context.getResources().getColor(R.color.huddle_body);
        } else {
            color = context.getResources().getColor(R.color.stream_body);
        }

        senderName.setBackgroundColor(color);
        contentView.setBackgroundColor(color);

        tile.setTag(R.id.messageID, message.getID());

        return tile;

    }

    public long getItemId(int position) {
        return this.getItem(position).getID();
    }

}
