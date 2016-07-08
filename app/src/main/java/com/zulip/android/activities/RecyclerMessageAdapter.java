package com.zulip.android.activities;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.viewholders.MessageHeaderParent;
import com.zulip.android.viewholders.MessageHolder;

import java.util.ArrayList;
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
        setupLists(messageList);
    }
    private int[] getHeaderAndNextIndex(String id) {
        //Return the next header, if this is the last header then returns the last index (loading view)
        int indices[] = {-1, -1};
        for (int i = 0; i < getItemCount(false); i++) {
            if (items.get(i) instanceof MessageHeaderParent) {
                MessageHeaderParent item = (MessageHeaderParent) items.get(i);
                if (indices[0] != -1) {
                    indices[1] = i;
                    return indices;
                }
                if (item.getId().equals(id)) {
                    indices[0] = i;
                }
            }
        }
        return indices;
    }

    private void setupLists(List<Message> messageList) {
        int headerParents = 0;
        for (int i = 0; i < messageList.size() - 1; i++) {
            headerParents = (addMessage(messageList.get(i), i + headerParents)) ? headerParents + 1 : headerParents;
        }
    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof MessageHeaderParent)
            return VIEWTYPE_MESSAGE_HEADER;
        else if (items.get(position) instanceof Message)
            return VIEWTYPE_MESSAGE;
        else if (getItem(position) instanceof Integer && (Integer) getItem(position) == VIEWTYPE_HEADER)
            return VIEWTYPE_HEADER;
        else if (getItem(position) instanceof Integer && (Integer) getItem(position) == VIEWTYPE_FOOTER)
            return VIEWTYPE_FOOTER;
        else {
            Log.e("ItemError", "object: " + items.get(position).toString());
            throw new RuntimeException("MESSAGE TYPE NOT KNOWN & Position:"+position);
        }
    }

    public boolean addMessage(Message message, int messageAndHeadersCount) {

        int[] index = getHeaderAndNextIndex(message.getIdForHolder());

        if (index[0] < 0) { //No messageParent for this one
            MessageHeaderParent messageHeaderParent = new MessageHeaderParent((message.getStream() == null) ? null : message.getStream().getName(), message.getSubject(), message.getIdForHolder());
            messageHeaderParent.setMessageType(message.getType());
            messageHeaderParent.setDisplayRecipent(message.getDisplayRecipient(zulipApp));
            if (message.getType() == MessageType.STREAM_MESSAGE) {
                messageHeaderParent.setMute(zulipApp.isTopicMute(message));
            }
            messageHeaderParent.setColor((message.getStream() == null) ? mDefaultStreamHeaderColor : message.getStream().getColor());
            items.add(messageAndHeadersCount + 1, messageHeaderParent); //1 for LoadingHeader
            notifyItemInserted(messageAndHeadersCount + 1);
            items.add(messageAndHeadersCount + 2, message);
            notifyItemInserted(messageAndHeadersCount + 2);
            return true;
        } else {
            int nextHeader = (index[1] != -1) ? index[1] : getItemCount(false);
            items.add(nextHeader, message);
            notifyItemInserted(nextHeader);
            return false;
        }
    }




    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEWTYPE_MESSAGE_HEADER:
                MessageHeaderParent.MessageHeaderHolder holder = new MessageHeaderParent.MessageHeaderHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_header, parent, false));
                holder.streamTextView.setText(privateHuddleText);
                holder.streamTextView.setTextColor(Color.WHITE);
                return holder;
            case VIEWTYPE_MESSAGE:
                View messageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_tile, parent, false);
                MessageHolder messageHolder = new MessageHolder(messageView);
                return messageHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEWTYPE_MESSAGE_HEADER:
                final MessageHeaderParent messageHeaderParent = (MessageHeaderParent) getItem(position);
                final MessageHeaderParent.MessageHeaderHolder messageHeaderHolder = ((MessageHeaderParent.MessageHeaderHolder) holder);

                if (messageHeaderParent.getMessageType() == MessageType.STREAM_MESSAGE) {
                    messageHeaderHolder.streamTextView.setText(messageHeaderParent.getStream());
                    messageHeaderHolder.topicTextView.setText(messageHeaderParent.getSubject());

                    ViewCompat.setBackgroundTintList(messageHeaderHolder.arrowHead, ColorStateList.valueOf(messageHeaderParent.getColor()));
                    messageHeaderHolder.streamTextView.setBackgroundColor(messageHeaderParent.getColor());

                    if (messageHeaderParent.isMute()) {
                        messageHeaderHolder.muteMessageImage.setVisibility(View.VISIBLE);
                    }

                } else { //PRIVATE MESSAGE
                    messageHeaderHolder.streamTextView.setText(privateHuddleText);
                    messageHeaderHolder.streamTextView.setTextColor(Color.WHITE);
                    messageHeaderHolder.topicTextView.setText(messageHeaderParent.getDisplayRecipent());
                    ViewCompat.setBackgroundTintList(messageHeaderHolder.arrowHead, ColorStateList.valueOf(mDefaultStreamHeaderColor));
                    messageHeaderHolder.streamTextView.setBackgroundColor(mDefaultStreamHeaderColor);
                }
                break;
            case VIEWTYPE_MESSAGE:

                MessageHolder messageHolder = ((MessageHolder) holder);
                final Message message = ((Message) items.get(position));
                messageHolder.contentView.setText(message.getFormattedContent(zulipApp));

                if (message.getType() == MessageType.STREAM_MESSAGE) {
                    messageHolder.senderName.setText(message.getSender().getName());
                    messageHolder.leftBar.setBackgroundColor(message.getStream().getColor());
                    messageHolder.messageTile.setBackgroundColor(Color.WHITE);
                } else {
                    messageHolder.senderName.setText(message.getSender().getName());
                    messageHolder.leftBar.setBackgroundColor(mDefaultPrivateMessageColor);
                    messageHolder.messageTile.setBackgroundColor(mDefaultPrivateMessageColor);
                }
                break;
        }
    }

    }
}
