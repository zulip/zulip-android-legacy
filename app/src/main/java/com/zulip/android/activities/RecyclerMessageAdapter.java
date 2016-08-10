package com.zulip.android.activities;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zulip.android.models.Stream;
import com.zulip.android.networking.AsyncPointerUpdate;

import com.squareup.picasso.Picasso;
import com.zulip.android.util.OnItemClickListener;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.filters.NarrowFilterStream;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.util.ZLog;
import com.zulip.android.viewholders.LoadingHolder;
import com.zulip.android.viewholders.MessageHeaderParent;
import com.zulip.android.viewholders.MessageHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RecyclerMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEWTYPE_MESSAGE_HEADER = 1;
    private static final int VIEWTYPE_MESSAGE = 2;
    public static final int VIEWTYPE_HEADER = 3; //At position 0
    private static final int VIEWTYPE_FOOTER = 4; //At end position
    private boolean startedFromFilter;
    private static String privateHuddleText;
    private List<Object> items;
    private ZulipApp zulipApp;
    private Context context;
    private NarrowListener narrowListener;
    private static final float HEIGHT_IN_DP = 48;
    private
    @ColorInt
    int mDefaultStreamHeaderColor;

    @ColorInt
    private int streamMessageBackground;

    @ColorInt
    private int privateMessageBackground;
    private OnItemClickListener onItemClickListener;
    private int contextMenuItemSelectedPosition;
    private View footerView;
    private View headerView;

    private boolean isCurrentThemeNight;

    int getContextMenuItemSelectedPosition() {
        return contextMenuItemSelectedPosition;
    }

    RecyclerMessageAdapter(List<Message> messageList, final Context context, boolean startedFromFilter) {
        super();
        items = new ArrayList<>();
        zulipApp = ZulipApp.get();
        this.context = context;
        narrowListener = (NarrowListener) context;
        this.startedFromFilter = startedFromFilter;
        isCurrentThemeNight = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        mDefaultStreamHeaderColor = ContextCompat.getColor(context, R.color.stream_header);
        privateMessageBackground = ContextCompat.getColor(context, R.color.private_background);
        streamMessageBackground = ContextCompat.getColor(context, R.color.stream_background);

        privateHuddleText = context.getResources().getString(R.string.huddle_text);
        setupHeaderAndFooterViews();
        onItemClickListener = new OnItemClickListener() {
            @Override
            public void onItemClick(int viewId, int position) {
                switch (viewId) {
                    case R.id.displayRecipient: //StreamTV
                        MessageHeaderParent messageHeaderParent = (MessageHeaderParent) getItem(position);
                        if (messageHeaderParent.getMessageType() == MessageType.PRIVATE_MESSAGE) {
                            narrowListener.onNarrow(new NarrowFilterPM(
                                    Arrays.asList(messageHeaderParent.getRecipients((ZulipApp.get())))));
                        } else {

                            narrowListener.onNarrow(new NarrowFilterStream(Stream.getByName(zulipApp, messageHeaderParent.getStream()), ""));
                            narrowListener.onNarrowFillSendBoxStream(messageHeaderParent.getStream(), "", false);
                        }
                        break;

                    case R.id.instance: //Topic
                        MessageHeaderParent messageParent = (MessageHeaderParent) getItem(position);
                        narrowListener.onNarrow(new NarrowFilterStream(Stream.getByName(zulipApp, messageParent.getStream()), messageParent.getSubject()));
                        narrowListener.onNarrowFillSendBoxStream(messageParent.getStream(), "", false);
                        break;
                    case R.id.contentView: //Main message
                        Message message = (Message) getItem(position);
                        narrowListener.onNarrowFillSendBox(message, false);
                        break;

                    case R.id.messageTile:
                        Message msg = (Message) getItem(position);
                        try {
                            int mID = msg.getID();
                            if (zulipApp.getPointer() < mID) {
                                (new AsyncPointerUpdate(zulipApp)).execute(mID);
                                zulipApp.setPointer(mID);
                            }
                        } catch (NullPointerException e) {
                            ZLog.logException(e);
                        }
                        break;
                    default:
                        Log.e("onItemClick", "Click listener not setup for: " + context.getResources().getResourceName(viewId) + " at position - " + position);
                }
            }

            @Override
            public Message getMessageAtPosition(int position) {
                if (getItem(position) instanceof Message) {
                    return (Message) getItem(position);
                }
                return null;
            }

            @Override
            public void setContextItemSelectedPosition(int adapterPosition) {
                contextMenuItemSelectedPosition = adapterPosition;
            }
        };
        setupLists(messageList);
    }

    private void setupHeaderAndFooterViews() {
        items.add(0, VIEWTYPE_HEADER); //Placeholder for header
        items.add(VIEWTYPE_FOOTER); //Placeholder for footer
        notifyItemInserted(0);
        notifyItemInserted(items.size() - 1);

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
        setFooterShowing(false);
        setHeaderShowing(false);
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
            throw new RuntimeException("MESSAGE TYPE NOT KNOWN & Position:" + position);
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


    public void addNewMessage(Message message) {
        MessageHeaderParent item = null;
        for (int i = getItemCount(false) - 1; i > 1; i--) {
            //Find the last header and check if it belongs to this message!
            if (items.get(i) instanceof MessageHeaderParent) {
                item = (MessageHeaderParent) items.get(i);
                if (!item.getId().equals(message.getIdForHolder())) {
                    item = null;
                }
                break;
            }
        }
        if (item == null) {
            item = new MessageHeaderParent((message.getStream() == null) ? null :
                    message.getStream().getName(), message.getSubject(), message.getIdForHolder());
            item.setMessageType(message.getType());
            item.setDisplayRecipent(message.getDisplayRecipient(zulipApp));
            if (message.getType() == MessageType.STREAM_MESSAGE)
                item.setMute(zulipApp.isTopicMute(message));
            item.setColor((message.getStream() == null) ? mDefaultStreamHeaderColor : message.getStream().getColor());
            items.add(getItemCount(true) - 1, item);
            notifyItemInserted(getItemCount(true) - 1);
        }
        items.add(getItemCount(true) - 1, message);
        notifyItemInserted(getItemCount(true) - 1);
    }


    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEWTYPE_MESSAGE_HEADER:
                MessageHeaderParent.MessageHeaderHolder holder = new MessageHeaderParent.MessageHeaderHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_header, parent, false));
                holder.streamTextView.setText(privateHuddleText);
                holder.streamTextView.setTextColor(Color.WHITE);
                holder.setOnItemClickListener(onItemClickListener);
                return holder;
            case VIEWTYPE_MESSAGE:
                View messageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_tile, parent, false);
                MessageHolder messageHolder = new MessageHolder(messageView);
                messageHolder.setItemClickListener(onItemClickListener);
                if (isCurrentThemeNight) {
                    messageHolder.leftBar.setVisibility(View.GONE);
                }
                return messageHolder;
            case VIEWTYPE_FOOTER:
                footerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_loading, parent, false);
                return new LoadingHolder(footerView);
            case VIEWTYPE_HEADER:
                headerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_loading, parent, false);
                LoadingHolder headerLoadingHolder = new LoadingHolder(headerView);
                setHeaderShowing(false);
                return headerLoadingHolder;
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

                    if (!isCurrentThemeNight) {
                        messageHeaderHolder.streamTextView.setBackgroundColor(messageHeaderParent.getColor());
                        ViewCompat.setBackgroundTintList(messageHeaderHolder.arrowHead, ColorStateList.valueOf(messageHeaderParent.getColor()));
                    }
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
                    if (!isCurrentThemeNight)
                        messageHolder.leftBar.setBackgroundColor(message.getStream().getColor());
                    messageHolder.messageTile.setBackgroundColor(streamMessageBackground);
                } else {
                    messageHolder.senderName.setText(message.getSender().getName());
                    if (!isCurrentThemeNight)
                        messageHolder.leftBar.setBackgroundColor(privateMessageBackground);
                    messageHolder.messageTile.setBackgroundColor(privateMessageBackground);
                }

                setUpGravatar(message, messageHolder);
                setUpTime(message, messageHolder);
                break;
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == VIEWTYPE_MESSAGE)
            markThisMessageAsRead((Message) getItem(holder.getAdapterPosition()));
    }

    private void markThisMessageAsRead(Message message) {
        try {
            int mID = message.getID();
            if (!startedFromFilter && zulipApp.getPointer() < mID) {
                (new AsyncPointerUpdate(zulipApp)).execute(mID);
                zulipApp.setPointer(mID);
            }
            zulipApp.markMessageAsRead(message);
        } catch (NullPointerException e) {
            Log.w("scrolling", "Could not find a location to scroll to!");
        }
    }

    private void setUpTime(Message message, MessageHolder messageHolder) {
        if (DateUtils.isToday(message.getTimestamp().getTime())) {
            messageHolder.timestamp.setText(DateUtils.formatDateTime(context, message
                    .getTimestamp().getTime(), DateUtils.FORMAT_SHOW_TIME));
        } else {
            messageHolder.timestamp.setText(DateUtils.formatDateTime(context, message
                    .getTimestamp().getTime(), DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_ABBREV_MONTH
                    | DateUtils.FORMAT_SHOW_TIME));
        }
    }


    private void setUpGravatar(Message message, MessageHolder messageHolder) {
        //Setup Gravatar
        Bitmap gravatarImg = ((ZulipActivity) context).getGravatars().get(message.getSender().getEmail());
        if (gravatarImg != null) {
            // Gravatar already exists for this image, set the ImageView to it
            messageHolder.gravatar.setImageBitmap(gravatarImg);
        } else {
            // From http://stackoverflow.com/questions/4605527/
            Resources resources = context.getResources();
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    35, resources.getDisplayMetrics());
            String url = message.getSender().getAvatarURL() + "&s=" + px;
            Picasso.with(context)
                    .load(url)
                    .placeholder(android.R.drawable.stat_notify_error)
                    .error(android.R.drawable.presence_online)
                    .into(messageHolder.gravatar);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void clear() {
        items.clear();
        setupHeaderAndFooterViews();
        notifyDataSetChanged();
    }

    public void remove(Message msg) {
        items.remove(msg);
        notifyDataSetChanged();
    }

    public int getItemIndex(Message message) {
        return items.indexOf(message);
    }

    public Object getItem(int position) {
        return items.get(position);
    }


    public int getItemCount(boolean includeFooter) {
        if (includeFooter) return getItemCount();
        else return getItemCount() - 1;
    }

    public void setFooterShowing(boolean show) {
        if (footerView == null) return;
        if (show) {
            final float scale = footerView.getContext().getResources().getDisplayMetrics().density;
            footerView.getLayoutParams().height = (int) (HEIGHT_IN_DP * scale + 0.5f);
            footerView.setVisibility(View.VISIBLE);
        } else {
            footerView.getLayoutParams().height = 0;
            footerView.setVisibility(View.GONE);
        }
    }

    public void setHeaderShowing(boolean show) {
        if (headerView == null) return;
        if (show) {
            final float scale = headerView.getContext().getResources().getDisplayMetrics().density;
            headerView.getLayoutParams().height = (int) (HEIGHT_IN_DP * scale + 0.5f);
            headerView.setVisibility(View.VISIBLE);
        } else {
            headerView.getLayoutParams().height = 0;
            headerView.setVisibility(View.GONE);
        }
    }
}
