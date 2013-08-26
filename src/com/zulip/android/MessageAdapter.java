package com.zulip.android;

import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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

    public View getView(int position, View convertView, ViewGroup group) {

        ZulipActivity context = (ZulipActivity) this.getContext();
        Message message = getItem(position);
        LinearLayout tile;

        if (convertView == null
                || !(convertView.getClass().equals(LinearLayout.class))) {
            // We didn't get passed a tile, so construct a new one.
            // In the future, we should inflate from a layout here.
            LayoutInflater inflater = ((Activity) this.getContext())
                    .getLayoutInflater();
            tile = (LinearLayout) inflater.inflate(R.layout.message_tile,
                    group, false);
        } else {
            tile = (LinearLayout) convertView;
        }

        LinearLayout envelopeTile = (LinearLayout) tile
                .findViewById(R.id.envelopeTile);
        TextView display_recipient = (TextView) tile
                .findViewById(R.id.displayRecipient);

        if (message.getType() == MessageType.STREAM_MESSAGE) {
            envelopeTile.setBackgroundResource(R.drawable.stream_header);

            Stream stream = message.getStream();
            if (stream != null) {
                envelopeTile.setBackgroundColor(stream.getColor());
            }
        } else {
            envelopeTile.setBackgroundResource(R.drawable.huddle_header);
        }

        if (message.getType() != MessageType.STREAM_MESSAGE) {
            display_recipient.setText(context.getString(R.string.huddle_text)
                    + " " + message.getDisplayRecipient(context.app));
            display_recipient.setTextColor(Color.WHITE);
        } else {
            display_recipient.setText(message.getDisplayRecipient(context.app));
            display_recipient.setTextColor(Color.BLACK);
        }

        TextView sep = (TextView) tile.findViewById(R.id.sep);
        TextView instance = (TextView) tile.findViewById(R.id.instance);

        if (message.getType() == MessageType.STREAM_MESSAGE) {
            instance.setVisibility(View.VISIBLE);
            sep.setVisibility(View.VISIBLE);
            instance.setText(message.getSubject());
        } else {
            instance.setVisibility(View.GONE);
            sep.setVisibility(View.GONE);
        }

        TextView senderName = (TextView) tile.findViewById(R.id.senderName);
        senderName.setText(message.getSender().getName());

        TextView contentView = (TextView) tile.findViewById(R.id.contentView);
        contentView.setText(message.getContent());

        TextView timestamp = (TextView) tile.findViewById(R.id.timestamp);

        if (DateUtils.isToday(message.getTimestamp().getTime())) {
            timestamp.setText(DateUtils.formatDateTime(context, message
                    .getTimestamp().getTime(), DateUtils.FORMAT_SHOW_TIME));
        } else {
            timestamp.setText(DateUtils.formatDateTime(context, message
                    .getTimestamp().getTime(), DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_ABBREV_MONTH
                    | DateUtils.FORMAT_SHOW_TIME));
        }

        ImageView gravatar = (ImageView) tile.findViewById(R.id.gravatar);
        Bitmap gravatar_img = context.gravatars.get(message.getSender()
                .getEmail());
        if (gravatar_img != null) {
            // Gravatar already exists for this image, set the ImageView to it
            gravatar.setImageBitmap(gravatar_img);
        } else {
            // Go get the Bitmap
            URL url = GravatarAsyncFetchTask.sizedURL(context, message
                    .getSender().getAvatarURL(), 35);
            GravatarAsyncFetchTask task = new GravatarAsyncFetchTask(context,
                    gravatar, message.getSender());
            task.loadBitmap(context, url, gravatar, message.getSender());
        }

        int color;

        if (message.getType() != MessageType.STREAM_MESSAGE) {
            color = context.getResources().getColor(R.color.huddle_body);
        } else {
            color = context.getResources().getColor(R.color.stream_body);
        }

        LinearLayout messageTile = (LinearLayout) tile
                .findViewById(R.id.messageTile);
        messageTile.setBackgroundColor(color);

        tile.setTag(R.id.messageID, message.getID());

        return tile;

    }

    public long getItemId(int position) {
        return this.getItem(position).getID();
    }

}
