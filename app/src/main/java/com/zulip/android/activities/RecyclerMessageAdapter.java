package com.zulip.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j256.ormlite.stmt.UpdateBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.filters.NarrowFilterStream;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.models.Person;
import com.zulip.android.models.Stream;
import com.zulip.android.util.MutedTopics;
import com.zulip.android.util.OnItemClickListener;
import com.zulip.android.util.UrlHelper;
import com.zulip.android.util.ZLog;
import com.zulip.android.viewholders.LoadingHolder;
import com.zulip.android.viewholders.MessageHeaderParent;
import com.zulip.android.viewholders.MessageHolder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * An adapter to bind the messages to a RecyclerView.
 * This has two main ViewTypes {@link MessageHeaderParent.MessageHeaderHolder} and {@link MessageHolder}
 * Each Message is inserted to its MessageHeader which are distinguished by the {@link Message#getIdForHolder()}
 * saved in {@link MessageHeaderParent#getId()}
 * <p>
 * There are two ways to insert a message in this adapter one {@link RecyclerMessageAdapter#addOldMessage(Message, int, StringBuilder)}
 * and second one {@link RecyclerMessageAdapter#addNewMessage(Message)}
 * The first one is used to add old messages from the databases with {@link com.zulip.android.util.MessageListener.LoadPosition#BELOW}
 * and {@link com.zulip.android.util.MessageListener.LoadPosition#INITIAL}. Messages are added from 1st index of the adapter and new
 * headerParents are created if it doesn't matches the current header where the adding is being placed, this is done to match the UI as the web.
 * In addNewMessages the messages are loaded in the bottom and new headers are created if it does not matches the last header.
 */
public class RecyclerMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEWTYPE_MESSAGE_HEADER = 1;
    public static final int VIEWTYPE_HEADER = 3; //At position 0
    private static final int VIEWTYPE_MESSAGE = 2;
    private static final int VIEWTYPE_FOOTER = 4; //At end position
    private static final float HEIGHT_IN_DP = 48;
    private static String privateHuddleText;
    private boolean startedFromFilter;
    private List<Object> items;
    private ZulipApp zulipApp;
    private MutedTopics mMutedTopics;
    private Context context;
    private NarrowListener narrowListener;
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
    private UpdateBuilder<Message, Object> updateBuilder;

    private boolean isCurrentThemeNight;
    private HashMap<Integer, Integer> defaultAvatarColorHMap;

    RecyclerMessageAdapter(List<Message> messageList, final Context context, boolean startedFromFilter) {
        super();
        items = new ArrayList<>();
        zulipApp = ZulipApp.get();
        mMutedTopics = MutedTopics.get();
        this.context = context;
        narrowListener = (NarrowListener) context;
        this.startedFromFilter = startedFromFilter;
        isCurrentThemeNight = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        mDefaultStreamHeaderColor = ContextCompat.getColor(context, R.color.stream_header);
        privateMessageBackground = ContextCompat.getColor(context, R.color.private_background);
        streamMessageBackground = ContextCompat.getColor(context, R.color.stream_background);

        defaultAvatarColorHMap = new HashMap<>();
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

                            narrowListener.onNarrow(new NarrowFilterStream(Stream.getByName(zulipApp, messageHeaderParent.getStream()), null));
                            narrowListener.onNarrowFillSendBoxStream(messageHeaderParent.getStream(), "", false);
                        }
                        break;
                    case R.id.instance: //Topic
                        MessageHeaderParent messageParent = (MessageHeaderParent) getItem(position);
                        if (messageParent.getMessageType() == MessageType.STREAM_MESSAGE) {
                            narrowListener.onNarrow(new NarrowFilterStream(Stream.getByName(zulipApp, messageParent.getStream()), messageParent.getSubject()));
                            narrowListener.onNarrowFillSendBoxStream(messageParent.getStream(), "", false);
                        } else {
                            Person[] recipentArray = messageParent.getRecipients(zulipApp);
                            narrowListener.onNarrow(new NarrowFilterPM(Arrays.asList(recipentArray)));
                            narrowListener.onNarrowFillSendBoxPrivate(recipentArray, false);
                        }
                        break;
                    case R.id.senderTile: // Sender Tile
                    case R.id.contentView: //Main message
                        Message message = (Message) getItem(position);
                        narrowListener.onNarrowFillSendBox(message, false);
                        break;
                    case R.id.messageTile:
                        Message msg = (Message) getItem(position);
                        try {
                            int mID = msg.getID();
                            if (zulipApp.getPointer() < mID) {
                                zulipApp.syncPointer(mID);
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
            public MessageHeaderParent getMessageHeaderParentAtPosition(int position) {
                if (getItem(position) instanceof MessageHeaderParent) {
                    return (MessageHeaderParent) getItem(position);
                }
                return null;
            }

            @Override
            public void setContextItemSelectedPosition(int adapterPosition) {
                contextMenuItemSelectedPosition = adapterPosition;
            }
        };
        setupLists(messageList);
        updateBuilder = zulipApp.getDao(Message.class).updateBuilder();
    }

    int getContextMenuItemSelectedPosition() {
        return contextMenuItemSelectedPosition;
    }

    /**
     * Add's a placeHolder value for Header and footer loading with values of 3-{@link #VIEWTYPE_HEADER} and 4-{@link #VIEWTYPE_FOOTER} respectively.
     * So that for these placeHolder can be created a ViewHolder in {@link #onCreateViewHolder(ViewGroup, int)}
     */
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
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < messageList.size() - 1; i++) {
            Message message = messageList.get(i);
            headerParents = (addOldMessage(message, i + headerParents, stringBuilder)) ? headerParents + 1 : headerParents;
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

    /**
     * Add an old message to the current list and add those messages to the existing messageHeaders if no
     * messageHeader is found then create a new messageHeader
     *
     * @param message                Message to be added
     * @param messageAndHeadersCount Count of the (messages + messageHeaderParent) added in the loop from where this function is being called
     * @param lastHolderId           This is StringBuilder so as to make pass by reference work, the new lastHolderId is saved here if the value changes
     * @return returns true if a new messageHeaderParent is created for this message so as to increment the count by where this function is being called.
     */
    public boolean addOldMessage(Message message, int messageAndHeadersCount, StringBuilder lastHolderId) {
        if (!lastHolderId.toString().equals(message.getIdForHolder()) || lastHolderId.toString().equals("")) {
            MessageHeaderParent messageHeaderParent = new MessageHeaderParent((message.getStream() == null) ? null : message.getStream().getName(), message.getSubject(), message.getIdForHolder(), message);
            messageHeaderParent.setMessageType(message.getType());
            messageHeaderParent.setDisplayRecipent(message.getDisplayRecipient(zulipApp));
            if (message.getType() == MessageType.STREAM_MESSAGE) {
                messageHeaderParent.setMute(mMutedTopics.isTopicMute(message));
            }
            messageHeaderParent.setColor((message.getStream() == null) ? mDefaultStreamHeaderColor : message.getStream().getParsedColor());
            items.add(messageAndHeadersCount + 1, messageHeaderParent); //1 for LoadingHeader
            notifyItemInserted(messageAndHeadersCount + 1);
            items.add(messageAndHeadersCount + 2, message);
            notifyItemInserted(messageAndHeadersCount + 2);
            lastHolderId.setLength(0);
            lastHolderId.append(messageHeaderParent.getId());
            return true;
        } else {
            items.add(messageAndHeadersCount + 1, message);
            notifyItemInserted(messageAndHeadersCount + 1);
            return false;
        }
    }

    /**
     * Add a new message to the bottom of the list and create a new messageHeaderParent if last did not match this message
     * Stream/subject or private recipients.
     *
     * @param message Message to be added
     */
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
                    message.getStream().getName(), message.getSubject(), message.getIdForHolder(), message);
            item.setMessageType(message.getType());
            item.setDisplayRecipent(message.getDisplayRecipient(zulipApp));
            if (message.getType() == MessageType.STREAM_MESSAGE)
                item.setMute(mMutedTopics.isTopicMute(message));
            item.setColor((message.getStream() == null) ? mDefaultStreamHeaderColor : message.getStream().getParsedColor());
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
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
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
                messageHolder.contentView.setMovementMethod(LinkMovementMethod.getInstance());

                final String url = message.extractImageUrl(zulipApp);
                if (url != null) {
                    messageHolder.contentImageContainer.setVisibility(View.VISIBLE);
                    Picasso.with(context).load(url)
                            .into(messageHolder.contentImage);

                    messageHolder.contentImageContainer
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.setData(Uri.parse(url));
                                    zulipApp.startActivity(i);
                                }
                            });
                } else {
                    messageHolder.contentImageContainer.setVisibility(View.GONE);
                    messageHolder.contentImage.setImageDrawable(null);
                }
                if (message.getType() == MessageType.STREAM_MESSAGE) {
                    messageHolder.senderName.setText(message.getSender().getName());
                    if (!isCurrentThemeNight)
                        messageHolder.leftBar.setBackgroundColor(message.getStream().getParsedColor());
                    messageHolder.messageTile.setBackgroundColor(streamMessageBackground);
                } else {
                    messageHolder.senderName.setText(message.getSender().getName());
                    if (!isCurrentThemeNight) {
                        messageHolder.leftBar.setBackgroundColor(privateMessageBackground);
                    }
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
        if (holder.getItemViewType() == VIEWTYPE_MESSAGE && !startedFromFilter)
            markThisMessageAsRead((Message) getItem(holder.getAdapterPosition()));
    }

    /**
     * This is called when the Message is bind to the Holder and attached, displayed in the window.
     *
     * @param message Mark this message read
     */
    private void markThisMessageAsRead(Message message) {
        try {
            int mID = message.getID();
            if (zulipApp.getPointer() < mID) {
                zulipApp.syncPointer(mID);
            }

            boolean isMessageRead = false;
            if (message.getMessageRead() != null) {
                isMessageRead = message.getMessageRead();
            }
            if (!isMessageRead) {
                try {
                    updateBuilder.where().eq(Message.ID_FIELD, message.getID());
                    updateBuilder.updateColumnValue(Message.MESSAGE_READ_FIELD, true);
                    updateBuilder.update();
                } catch (SQLException e) {
                    ZLog.logException(e);
                }
                zulipApp.markMessageAsRead(message);
            }
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


    private void setUpGravatar(final Message message, final MessageHolder messageHolder) {
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
            url = UrlHelper.addHost(url);

            Picasso.with(context)
                    .load(url)
                    .placeholder(android.R.drawable.stat_notify_error)
                    .error(android.R.drawable.presence_online)
                    .into(messageHolder.gravatar, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {
                            int hMapKey = message.getSender().getId();
                            int avatarColor;

                            // check if current sender has already been allotted a randomly generated color
                            if (defaultAvatarColorHMap.containsKey(hMapKey)) {
                                avatarColor = defaultAvatarColorHMap.get(hMapKey);
                            } else {
                                // generate a random color for current sender id
                                avatarColor = getRandomColor(Color.rgb(255, 255, 255));

                                // add sender id and randomly generated color to hashmap
                                defaultAvatarColorHMap.put(hMapKey, avatarColor);
                            }
                            // square default avatar drawable
                            final GradientDrawable defaultAvatar = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.default_avatar);
                            defaultAvatar.setColor(avatarColor);
                            messageHolder.gravatar.setImageDrawable(defaultAvatar);
                        }
                    });
        }
    }

    /**
     * Method to generate random saturated colors for default avatar {@link R.drawable#default_avatar}
     *
     * @param mix integer color is mixed with randomly generated red, blue, green colors
     * @return a randomly generated color
     */
    private int getRandomColor(int mix) {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        // mix the color
        red = (red + Color.red(mix)) / 2;
        green = (green + Color.green(mix)) / 2;
        blue = (blue + Color.blue(mix)) / 2;

        int color = Color.rgb(red, green, blue);
        return color;
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

    public int getItemIndex(Message message) {
        return items.indexOf(message);
    }

    public int getItemIndex(int id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Message && ((Message) items.get(i)).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public Object getItem(int position) {
        return items.get(position);
    }

    /**
     * Return the size of the list with including or excluding footer
     *
     * @param includeFooter true to return the size including footer or false to return size excluding footer.
     * @return size of list
     */
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
