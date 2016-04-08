package com.zulip.android.activities;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zulip.android.util.CustomHtmlToSpannedConverter;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.filters.NarrowFilterStream;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.GravatarAsyncFetchTask;

/**
 * Adapter which stores Messages in a view, and generates LinearLayouts for
 * consumption by the ListView which displays the view.
 */
public class MessageAdapter extends ArrayAdapter<Message> {

    private static final HTMLSchema schema = new HTMLSchema();

    private @ColorInt int mDefaultStreamHeaderColor;
    private @ColorInt int mDefaultHuddleHeaderColor;
    private @ColorInt int mDefaultStreamMessageColor;
    private @ColorInt int mDefaultHuddleMessageColor;

    public MessageAdapter(Context context, List<Message> objects) {
        super(context, 0, objects);
        mDefaultStreamHeaderColor = ContextCompat.getColor(context, R.color.stream_header);
        mDefaultStreamMessageColor = ContextCompat.getColor(context, R.color.stream_body);
        mDefaultHuddleHeaderColor = ContextCompat.getColor(context, R.color.huddle_header);
        mDefaultHuddleMessageColor = ContextCompat.getColor(context, R.color.huddle_body);
    }

    public View getView(int position, View convertView, ViewGroup group) {

        final ZulipActivity context = (ZulipActivity) this.getContext();
        final Message message = getItem(position);
        LinearLayout tile;

        if (convertView == null || !(convertView.getClass().equals(LinearLayout.class))) {
            // We didn't get passed a tile, so construct a new one.
            // In the future, we should inflate from a layout here.
            LayoutInflater inflater = ((Activity) this.getContext()).getLayoutInflater();
            tile = (LinearLayout) inflater.inflate(R.layout.message_tile, group, false);
        } else {
            tile = (LinearLayout) convertView;
        }

        LinearLayout envelopeTile = (LinearLayout) tile.findViewById(R.id.envelopeTile);
        TextView display_recipient = (TextView) tile.findViewById(R.id.displayRecipient);
        ImageView muteImageView = (ImageView) tile.findViewById(R.id.muteMessageImage);

        if (message.getType() != MessageType.STREAM_MESSAGE) {
            envelopeTile.setBackgroundColor(mDefaultHuddleHeaderColor);
        } else {
            Stream stream = message.getStream();
            @ColorInt int color = stream == null ? mDefaultStreamHeaderColor : stream.getColor();
            envelopeTile.setBackgroundColor(color);
        }

        if (message.getType() != MessageType.STREAM_MESSAGE) {
            display_recipient.setText(context.getString(R.string.huddle_text, message.getDisplayRecipient(context.app)));
            display_recipient.setTextColor(Color.WHITE);
            display_recipient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getContext() instanceof NarrowListener) {
                        ((NarrowListener) getContext()).onNarrow(new NarrowFilterPM(
                                Arrays.asList(message.getRecipients((ZulipApp.get())))));
                    }
                }
            });
        } else {
            display_recipient.setText(message.getDisplayRecipient(context.app));
            display_recipient.setTextColor(Color.BLACK);
            display_recipient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getContext() instanceof NarrowListener) {
                        ((NarrowListener) getContext()).onNarrow(new NarrowFilterStream(message.getStream(), null));
                    }
                }
            });
            if (getContext() instanceof NarrowListener) {
                if (context.app.isTopicMute(message)) muteImageView.setVisibility(View.VISIBLE);
                else muteImageView.setVisibility(View.GONE);
            }
        }

        TextView sep = (TextView) tile.findViewById(R.id.sep);
        TextView instance = (TextView) tile.findViewById(R.id.instance);

        if (message.getType() != MessageType.STREAM_MESSAGE) {
            instance.setVisibility(View.GONE);
            sep.setVisibility(View.GONE);
            instance.setOnClickListener(null);
        } else {
            instance.setVisibility(View.VISIBLE);
            sep.setVisibility(View.VISIBLE);
            instance.setText(message.getSubject());
            instance.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getContext() instanceof NarrowListener) {
                        ((NarrowListener) getContext()).onNarrow(new NarrowFilterStream(message.getStream(), message.getSubject()));
                        ((NarrowListener) getContext()).onNarrowFillSendBox(message);
                    }
                }
            });
        }

        LinearLayout messageTile = (LinearLayout) tile.findViewById(R.id.messageTile);
        if (message.getType() != MessageType.STREAM_MESSAGE) {
            messageTile.setBackgroundColor(mDefaultHuddleMessageColor);
        } else {
            messageTile.setBackgroundColor(mDefaultStreamMessageColor);
        }

        TextView senderName = (TextView) tile.findViewById(R.id.senderName);
        senderName.setText(message.getSender().getName());

        TextView contentView = (TextView) tile.findViewById(R.id.contentView);

        Spanned formattedMessage = formatContent(message.getFormattedContent(),
                context.app);
        while (formattedMessage.length() != 0
                && formattedMessage.charAt(formattedMessage.length() - 1) == '\n') {
            formattedMessage = (Spanned) formattedMessage.subSequence(0,
                    formattedMessage.length() - 2);
        }
        contentView.setText(formattedMessage);

        contentView.setMovementMethod(LinkMovementMethod.getInstance());
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((NarrowListener) getContext()).onNarrowFillSendBox(message);
            }
        });

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
        Bitmap gravatar_img = context.getGravatars().get(message.getSender().getEmail());
        if (gravatar_img != null) {
            // Gravatar already exists for this image, set the ImageView to it
            gravatar.setImageBitmap(gravatar_img);
        } else {
            // Go get the Bitmap
            URL url = GravatarAsyncFetchTask.sizedURL(context, message.getSender().getAvatarURL(), 35);
            GravatarAsyncFetchTask task = new GravatarAsyncFetchTask(context, gravatar, message.getSender());
            task.loadBitmap(context, url, gravatar, message.getSender());
        }

        tile.setTag(R.id.messageID, message.getID());

        return tile;

    }

    /**
     * Copied from Html.fromHtml
     *
     * @param source HTML to be formatted
     * @param app
     * @return Span
     */
    public static Spanned formatContent(String source, ZulipApp app) {
        final Context context = app.getApplicationContext();
        final float density = context.getResources().getDisplayMetrics().density;
        Parser parser = new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, schema);
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }

        Html.ImageGetter emojiGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                int lastIndex = -1;
                if (source != null) {
                    lastIndex = source.lastIndexOf('/');
                }
                if (lastIndex != -1) {
                    String filename = source.substring(lastIndex + 1);
                    try {
                        Drawable drawable = Drawable.createFromStream(context
                                        .getAssets().open("emoji/" + filename),
                                "emoji/" + filename);
                        // scaling down by half to fit well in message
                        double scaleFactor = 0.5;
                        drawable.setBounds(0, 0,
                                (int) (drawable.getIntrinsicWidth()
                                        * scaleFactor * density),
                                (int) (drawable.getIntrinsicHeight()
                                        * scaleFactor * density));
                        return drawable;
                    } catch (IOException e) {
                        Log.e("MessageAdapter", e.getMessage());
                    }
                }
                return null;
            }
        };

        CustomHtmlToSpannedConverter converter = new CustomHtmlToSpannedConverter(
                source, null, null, parser, emojiGetter, app.getServerURI());
        return converter.convert();
    }

    public long getItemId(int position) {
        return this.getItem(position).getID();
    }

}
