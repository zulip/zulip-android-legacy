package com.zulip.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.j256.ormlite.stmt.UpdateBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.filters.NarrowFilterStream;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageDateSeparator;
import com.zulip.android.models.MessageType;
import com.zulip.android.models.Person;
import com.zulip.android.models.Reaction;
import com.zulip.android.models.Stream;
import com.zulip.android.util.ActivityTransitionAnim;
import com.zulip.android.util.Constants;
import com.zulip.android.util.ConvertDpPx;
import com.zulip.android.util.DateMethods;
import com.zulip.android.util.MutedTopics;
import com.zulip.android.util.OnItemClickListener;
import com.zulip.android.util.UrlHelper;
import com.zulip.android.util.ZLog;
import com.zulip.android.viewholders.LoadingHolder;
import com.zulip.android.viewholders.MessageHeaderParent;
import com.zulip.android.viewholders.MessageHolder;
import com.zulip.android.viewholders.stickyheaders.RetrieveHeaderView;
import com.zulip.android.viewholders.stickyheaders.interfaces.StickyHeaderHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.zulip.android.util.ConvertDpPx.convertDpToPixel;

/**
 * An adapter to bind the messages to a RecyclerView.
 * This has two main ViewTypes {@link MessageHeaderParent.MessageHeaderHolder} and {@link MessageHolder}
 * Each Message is inserted to its MessageHeader which are distinguished by the {@link Message#getIdForHolder()}
 * saved in {@link MessageHeaderParent#getId()}
 * <p>
 * There are two ways to insert a message in this adapter one {@link RecyclerMessageAdapter#addOldMessage(Message, int, StringBuilder,MessageDateSeparator)}
 * and second one {@link RecyclerMessageAdapter#addNewMessage(Message)}
 * The first one is used to add old messages from the databases with {@link com.zulip.android.util.MessageListener.LoadPosition#BELOW}
 * and {@link com.zulip.android.util.MessageListener.LoadPosition#INITIAL}. Messages are added from 1st index of the adapter and new
 * headerParents are created if it doesn't matches the current header where the adding is being placed, this is done to match the UI as the web.
 * In addNewMessages the messages are loaded in the bottom and new headers are created if it does not matches the last header.
 */
public class RecyclerMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StickyHeaderHandler{

    public static final int VIEWTYPE_MESSAGE_HEADER = 1;
    public static final int VIEWTYPE_HEADER = 3; //At position 0
    private static final int VIEWTYPE_MESSAGE = 2;
    private static final int VIEWTYPE_FOOTER = 4; //At end position
    private static final int VIEWTYPE_DATE_SEPARATOR = 5;
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
    private int contextMenuItemSelectedPosition = RecyclerView.NO_POSITION;
    private View footerView;
    private View headerView;
    private UpdateBuilder<Message, Object> updateBuilder;

    private boolean isCurrentThemeNight;
    private HashMap<Integer, Integer> defaultAvatarColorHMap;

    // position of view (MessageHeaderParent) which float's on top
    private int attachedHeaderAdapterPosition = RetrieveHeaderView.DEFAULT_VIEW_TYPE;

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
                        if (position == RetrieveHeaderView.DEFAULT_VIEW_TYPE) {
                            //clicked on floating header
                            if (attachedHeaderAdapterPosition != RetrieveHeaderView.DEFAULT_VIEW_TYPE) {
                                position = attachedHeaderAdapterPosition;
                            } else {
                                return;
                            }
                        }
                        MessageHeaderParent messageHeaderParent = (MessageHeaderParent) getItem(position);
                        if (messageHeaderParent.getMessageType() == MessageType.PRIVATE_MESSAGE) {
                            Person[] recipientArray = messageHeaderParent.getRecipients(zulipApp);
                            narrowListener.onNarrow(new NarrowFilterPM(Arrays.asList(recipientArray)),
                                    messageHeaderParent.getMessageId());
                            narrowListener.onNarrowFillSendBoxPrivate(recipientArray, false);
                        } else {
                            narrowListener.onNarrow(new NarrowFilterStream(Stream.getByName(zulipApp,
                                    messageHeaderParent.getStream()), null),
                                    messageHeaderParent.getMessageId());
                            narrowListener.onNarrowFillSendBoxStream(messageHeaderParent.getStream(), "", false);
                        }
                        break;
                    case R.id.instance: //Topic
                        if (position == RetrieveHeaderView.DEFAULT_VIEW_TYPE) {
                            //clicked on floating header
                            if (attachedHeaderAdapterPosition != RetrieveHeaderView.DEFAULT_VIEW_TYPE) {
                                position = attachedHeaderAdapterPosition;
                            } else {
                                return;
                            }
                        }
                        MessageHeaderParent messageParent = (MessageHeaderParent) getItem(position);
                        if (messageParent.getMessageType() == MessageType.STREAM_MESSAGE) {
                            narrowListener.onNarrow(new NarrowFilterStream(Stream.getByName(zulipApp,
                                    messageParent.getStream()), messageParent.getSubject()),
                                    messageParent.getMessageId());
                            narrowListener.onNarrowFillSendBoxStream(messageParent.getStream(), messageParent.getSubject(), false);
                        } else {
                            Person[] recipentArray = messageParent.getRecipients(zulipApp);
                            narrowListener.onNarrow(new NarrowFilterPM(Arrays.asList(recipentArray)),
                                    messageParent.getMessageId());
                            narrowListener.onNarrowFillSendBoxPrivate(recipentArray, false);
                        }
                        break;
                    case R.id.senderTile: // Sender Tile
                    case R.id.contentView: //Main message
                        Message message = (Message) getItem(position);
                        narrowListener.onNarrowFillSendBox(message, true);
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
        int dateSeparator = 0;
        Calendar calendar = null;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < messageList.size() - 1; i++) {
            Message message = messageList.get(i);
            //check for date separator
            if (calendar == null || !DateMethods.isSameDay(calendar.getTime(), message.getTimestamp())) {
                MessageDateSeparator separator = new MessageDateSeparator((calendar == null) ? null : calendar.getTime(), message.getTimestamp());
                calendar = Calendar.getInstance();
                calendar.setTime(message.getTimestamp());
                headerParents = (addOldMessage(message, dateSeparator + i + headerParents, stringBuilder, separator)) ? headerParents + 1 : headerParents;
                dateSeparator++;
            } else {
                headerParents = (addOldMessage(message, dateSeparator + i + headerParents, stringBuilder, null)) ? headerParents + 1 : headerParents;
            }

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
        else if (items.get(position) instanceof MessageDateSeparator)
            return VIEWTYPE_DATE_SEPARATOR;
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
    public boolean addOldMessage(Message message, int messageAndHeadersCount, StringBuilder lastHolderId, MessageDateSeparator dateSeparator) {
        //check for date separator
        if (dateSeparator != null) {
            addNewDateSeparator(dateSeparator, messageAndHeadersCount + 1); //1 for LoadingHeader
        }
        if (!lastHolderId.toString().equals(message.getIdForHolder()) || lastHolderId.toString().equals("") || dateSeparator != null) {
            MessageHeaderParent messageHeaderParent = new MessageHeaderParent((message.getStream() == null) ? null : message.getStream().getName(), message.getSubject(), message.getIdForHolder(), message);
            messageHeaderParent.setMessageType(message.getType());
            messageHeaderParent.setMessagesDate(message.getTimestamp());
            messageHeaderParent.setDisplayRecipent(message.getDisplayRecipient(zulipApp));
            if (message.getType() == MessageType.STREAM_MESSAGE) {
                messageHeaderParent.setMute(mMutedTopics.isTopicMute(message));
            }
            messageHeaderParent.setColor((message.getStream() == null) ? mDefaultStreamHeaderColor : message.getStream().getParsedColor());
            //1 for LoadingHeader
            //check for date separator
            items.add((dateSeparator != null) ? messageAndHeadersCount + 2 : messageAndHeadersCount + 1, messageHeaderParent);
            notifyItemInserted((dateSeparator != null) ? messageAndHeadersCount + 2 : messageAndHeadersCount + 1);
            items.add((dateSeparator != null) ? messageAndHeadersCount + 3 : messageAndHeadersCount + 2, message);
            notifyItemInserted((dateSeparator != null) ? messageAndHeadersCount + 3 : messageAndHeadersCount + 2);
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
        //check for date separator
        Date lastSeparatorDate = getLastSeparatorRightDate();
        if (lastSeparatorDate == null || !DateMethods.isSameDay(lastSeparatorDate, message.getTimestamp())) {
            addNewDateSeparator(new MessageDateSeparator(lastSeparatorDate, message.getTimestamp()), getItemCount(true) - 1);
        }
        MessageHeaderParent item = null;
        for (int i = getItemCount(false) - 1; i >= 1; i--) {
            //Find the last header and check if it belongs to this message!
            if (items.get(i) instanceof MessageHeaderParent) {
                item = (MessageHeaderParent) items.get(i);
                if (!item.getId().equals(message.getIdForHolder())
                        || item.getMessagesTimestamp() == null
                        || !DateMethods.isSameDay(item.getMessagesTimestamp(), message.getTimestamp())) {
                    item = null;
                }
                break;
            }
        }
        if (item == null) {
            item = createMessageHeader(message);
            items.add(getItemCount(true) - 1, item);
            notifyItemInserted(getItemCount(true) - 1);
        }
        items.add(getItemCount(true) - 1, message);
        notifyItemInserted(getItemCount(true) - 1);
    }

    public void addNewHeader(int position, Message message) {
        MessageHeaderParent item = createMessageHeader(message);
        items.add(position, item);
        notifyItemInserted(position);

        if (getItem(position + 2) instanceof Message) {
            // insert header with old topic for this message
            Message prevMessage = (Message) getItem(position + 2);
            MessageHeaderParent prevHeader = createMessageHeader(prevMessage);
            items.add(position + 2, prevHeader);
            notifyItemInserted(position + 2);
        }
    }

    private MessageHeaderParent createMessageHeader(Message message) {
        MessageHeaderParent header = new MessageHeaderParent((message.getStream() == null) ? null :
                message.getStream().getName(), message.getSubject(), message.getIdForHolder(), message);
        header.setMessageType(message.getType());
        header.setDisplayRecipent(message.getDisplayRecipient(zulipApp));
        header.setMessagesDate(message.getTimestamp());
        if (message.getType() == MessageType.STREAM_MESSAGE)
            header.setMute(mMutedTopics.isTopicMute(message));
        header.setColor((message.getStream() == null) ? mDefaultStreamHeaderColor : message.getStream().getParsedColor());

        return header;
    }

    /**
     * Add's date separator at position
     *
     * @param separator which we want to add
     * @param position  add separator at this position in list
     */
    private void addNewDateSeparator(MessageDateSeparator separator, int position) {
        items.add(position, separator);
        notifyItemInserted(position);
    }

    private Date getLastSeparatorRightDate() {
        //get last date separator
        MessageDateSeparator separator;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i) instanceof MessageDateSeparator) {
                separator = (MessageDateSeparator) items.get(i);
                return separator.getBelowMessageDate();
            }
        }
        return null;
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
            case VIEWTYPE_DATE_SEPARATOR:
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_date_separator, parent, false);
                return new DateSeparatorHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder,  int pos) {
        final int position = pos;
        switch (getItemViewType(position)) {
            case VIEWTYPE_MESSAGE_HEADER:
                final MessageHeaderParent messageHeaderParent = (MessageHeaderParent) getItem(position);
                final MessageHeaderParent.MessageHeaderHolder messageHeaderHolder = ((MessageHeaderParent.MessageHeaderHolder) holder);

                //set date
                messageHeaderHolder.timestamp.setText(DateMethods.getStringDate(messageHeaderParent.getMessagesTimestamp()));
                if (messageHeaderParent.getMessageType() == MessageType.STREAM_MESSAGE) {
                    messageHeaderHolder.streamTextView.setText(messageHeaderParent.getStream());

                    // update MessageHeaderParent subject when topic is updated
                    if (!messageHeaderParent.getSubject().equalsIgnoreCase(messageHeaderParent.getMessage().getSubject())) {
                        messageHeaderParent.setSubject(messageHeaderParent.getMessage().getSubject());
                    }
                    messageHeaderHolder.topicTextView.setText(messageHeaderParent.getSubject());

                    //set on long press
                    messageHeaderHolder.streamTextView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            messageHeaderHolder.streamTextView.setMaxLines(Integer.MAX_VALUE);
                            messageHeaderHolder.streamTextView.setEllipsize(null);
                            ((MessageHeaderParent) getItem(position)).setStreamExpanded(true);
                            return true;
                        }
                    });

                    messageHeaderHolder.topicTextView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            messageHeaderHolder.topicTextView.setMaxLines(Integer.MAX_VALUE);
                            messageHeaderHolder.topicTextView.setEllipsize(null);
                            ((MessageHeaderParent) getItem(position)).setTopicExpanded(true);
                            return true;
                        }
                    });

                    //if user have expanded, preserve them
                    if (messageHeaderParent.isStreamExpanded()) {
                        messageHeaderHolder.streamTextView.setMaxLines(Integer.MAX_VALUE);
                        messageHeaderHolder.streamTextView.setEllipsize(null);
                    } else {
                        messageHeaderHolder.streamTextView.setMaxLines(1);
                        messageHeaderHolder.streamTextView.setEllipsize(TextUtils.TruncateAt.END);
                    }

                    if (messageHeaderParent.isTopicExpanded()) {
                        messageHeaderHolder.topicTextView.setMaxLines(Integer.MAX_VALUE);
                        messageHeaderHolder.topicTextView.setEllipsize(null);
                    } else {
                        messageHeaderHolder.topicTextView.setMaxLines(1);
                        messageHeaderHolder.topicTextView.setEllipsize(TextUtils.TruncateAt.END);
                    }

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
                messageHolder.contentView.setLinkTextColor(ContextCompat.getColor(context, R.color.link_color));
                messageHolder.contentView.setMovementMethod(LinkMovementMethod.getInstance());

                int padding = convertDpToPixel(4);
                messageHolder.contentView.setShadowLayer(padding, 0, 0, 0);

                final String url = message.extractImageUrl(zulipApp);
                if (url != null) {
                    messageHolder.contentImageContainer.setVisibility(View.VISIBLE);
                    Picasso.with(context).load(url)
                            .into(messageHolder.contentImage);

                    messageHolder.contentImageContainer
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent i = new Intent(zulipApp.getApplicationContext(), PhotoViewActivity.class);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.putExtra(Intent.EXTRA_TEXT, url);
                                    zulipApp.startActivity(i);

                                    // activity transition animation
                                    ActivityTransitionAnim.transition(context);
                                }
                            });
                } else {
                    messageHolder.contentImageContainer.setVisibility(View.GONE);
                    messageHolder.contentImage.setImageDrawable(null);
                }
                Message lastMessage = getLastMessage(position);
                boolean isDifferentSender = (position == 0 || lastMessage == null || !lastMessage.getSender().equals(message.getSender()));
                if (message.getType() == MessageType.STREAM_MESSAGE) {
                    if (isDifferentSender) {
                        messageHolder.senderName.setVisibility(View.VISIBLE);
                        messageHolder.senderName.setText(message.getSender().getName());
                    } else {
                        messageHolder.senderName.setVisibility(View.GONE);
                    }
                    if (!isCurrentThemeNight)
                        messageHolder.leftBar.setBackgroundColor(message.getStream().getParsedColor());
                    messageHolder.messageTile.setBackgroundColor(streamMessageBackground);
                } else {
                    if (isDifferentSender) {
                        messageHolder.senderName.setVisibility(View.VISIBLE);
                        messageHolder.senderName.setText(message.getSender().getName());
                    } else {
                        messageHolder.senderName.setVisibility(View.GONE);
                    }
                    if (!isCurrentThemeNight) {
                        messageHolder.leftBar.setBackgroundColor(privateMessageBackground);
                    }
                    messageHolder.messageTile.setBackgroundColor(privateMessageBackground);
                }

                Boolean isEdited = message.isHasBeenEdited();
                if (isDifferentSender) {
                    messageHolder.gravatar.setVisibility(View.VISIBLE);
                    setUpGravatar(message, messageHolder);

                    // set visibility of edited tag
                    if (isEdited != null && isEdited) {
                        messageHolder.edited.setVisibility(View.VISIBLE);
                    } else {
                        messageHolder.edited.setVisibility(View.GONE);
                    }
                    messageHolder.leftEdited.setVisibility(View.GONE);

                    setUpTime(message, messageHolder.timestamp);
                    setUpStar(message, messageHolder.starImage);

                    //hide other one's
                    messageHolder.leftTimestamp.setVisibility(View.GONE);
                    messageHolder.leftStarImage.setVisibility(View.GONE);
                } else {
                    messageHolder.gravatar.setVisibility(View.GONE);

                    // set visibility of edited tag
                    if (isEdited != null && isEdited) {
                        messageHolder.leftEdited.setVisibility(View.VISIBLE);
                    } else {
                        messageHolder.leftEdited.setVisibility(View.GONE);
                    }
                    messageHolder.edited.setVisibility(View.GONE);

                    //check if duration between last and this message is less then hide
                    if (Math.abs(message.getTimestamp().getTime() - lastMessage.getTimestamp().getTime()) < Constants.HIDE_TIMESTAMP_THRESHOLD) {
                        messageHolder.leftTimestamp.setVisibility(View.GONE);
                    } else {
                        setUpTime(message, messageHolder.leftTimestamp);
                    }
                    setUpStar(message, messageHolder.leftStarImage);

                    //hide other one's
                    messageHolder.timestamp.setVisibility(View.GONE);
                    messageHolder.starImage.setVisibility(View.GONE);
                }
                setUpReactions(messageHolder, message);
                break;
            case VIEWTYPE_DATE_SEPARATOR:
                MessageDateSeparator messageDateSeparator = (MessageDateSeparator) items.get(position);
                DateSeparatorHolder dateSeparatorHolder = (DateSeparatorHolder) holder;
                if (!TextUtils.isEmpty(messageDateSeparator.getRightText())) {
                    dateSeparatorHolder.tvBelowMessagesDate.setText(messageDateSeparator.getRightText());
                } else {
                    dateSeparatorHolder.tvBelowMessagesDate.setVisibility(View.GONE);
                }
                if (!TextUtils.isEmpty(messageDateSeparator.getLeftText())) {
                    dateSeparatorHolder.tvAboveMessagesDate.setText(messageDateSeparator.getLeftText());
                } else {
                    dateSeparatorHolder.tvAboveMessagesDate.setVisibility(View.GONE);
                }
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == VIEWTYPE_MESSAGE)
            // mark fields as read in homeview and streams narrow
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
            if (!startedFromFilter && zulipApp.getPointer() < mID) {
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

    private void setUpTime(Message message, TextView timestamp) {
        timestamp.setText(DateUtils.formatDateTime(context, message
                .getTimestamp().getTime(), DateUtils.FORMAT_SHOW_TIME));
        timestamp.setVisibility(View.VISIBLE);
    }

    private void setUpStar(Message message, ImageView starImage) {
        if (message.getFlags() != null) {
            if (message.getFlags().contains("starred")) {
                message.setMessageStar(true);
            }
        }
        //Checking for a starred message
        if (message.getMessageStar()) {
            //Make star's imageView visibility to VISIBLE
            starImage.setVisibility(View.VISIBLE);
        } else {
            //Make star's imageView visibility to GONE
            starImage.setVisibility(View.GONE);
        }
    }

    private void setUpReactions(MessageHolder messageHolder, Message message) {
        try {
            messageHolder.reactionsTable.removeAllViews();
            if (message.getReactions().isEmpty()) {
                return;
            }

            // Calculate number of reactions in each row
            int messageWidth = messageHolder.messageTile.getContext().getResources().getDisplayMetrics().widthPixels;
            int reactionWidth = convertDpToPixel(Constants.REACTION_MARGIN);
            int numOfReactions = messageWidth / reactionWidth;
            TableRow row = new TableRow(messageHolder.messageTile.getContext());

            // set margin of 6dp between reactions in a table row
            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
            int margin = ConvertDpPx.convertDpToPixel(6);
            layoutParams.setMargins(margin, margin, margin, margin);

            // table row index
            int index = 0;
            for (Map.Entry<String, Integer> reaction : getDisplayReactions(message.getReactions()).entrySet()) {
                if (numOfReactions == 0) {
                    // current row is full
                    messageHolder.reactionsTable.addView(row, index++);
                    row = new TableRow(messageHolder.messageTile.getContext());
                    numOfReactions = messageWidth / reactionWidth;
                }

                // inflate reaction layout
                LinearLayout reactionTile = (LinearLayout) LayoutInflater.from(messageHolder.messageTile.getContext()).inflate(R.layout.reaction_tile, null);
                reactionTile.setLayoutParams(layoutParams);

                // emoji view in reaction
                ImageView imageView = (ImageView) reactionTile.findViewById(R.id.reaction_emoji);
                // emoji count view
                TextView textView = (TextView) reactionTile.findViewById(R.id.reaction_count);

                // get emoji drawable from assets
                String emojiName = reaction.getKey() + ".png";
                Drawable drawable = Drawable.createFromStream(zulipApp.getAssets().open("emoji/" + emojiName),
                        "emoji/" + emojiName);

                // shrink drawable resource
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                int size = ConvertDpPx.convertDpToPixel(20);
                drawable = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, size, size, true));

                imageView.setImageDrawable(drawable);
                textView.setText(String.format(Locale.getDefault(), "%d", reaction.getValue()));
                row.addView(reactionTile);

                numOfReactions--;
            }
            messageHolder.reactionsTable.addView(row, index);
        } catch (NullPointerException e) {
            Log.e("adapter", "message reactions are null");
        } catch (IOException e) {
            ZLog.logException(e);
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


    public HashMap<String, Integer> getDisplayReactions(List<Reaction> reactions) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        for (Reaction reaction : reactions) {
            Integer count = hashMap.get(reaction.getEmoji());
            hashMap.put(reaction.getEmoji(), (count != null) ? count + 1 : 1);
        }
        return hashMap;
    }

    @Override
    public List<?> getAdapterData() {
        return items;
    }

    @Override
    public void setAttachedHeader(int adapterPosition) {
        this.attachedHeaderAdapterPosition = adapterPosition;

    }

    private Message getLastMessage(int position) {
        if (position == 0)
            return null;
        Object object = getItem(position - 1);
        if (object instanceof Message) {
            return (Message) object;
        } else {
            return null;
        }
    }

    private class DateSeparatorHolder extends RecyclerView.ViewHolder {

        private TextView tvAboveMessagesDate, tvBelowMessagesDate;

        DateSeparatorHolder(View itemView) {
            super(itemView);
            tvAboveMessagesDate = (TextView) itemView.findViewById(R.id.tvAboveMessagesDate);
            tvBelowMessagesDate = (TextView) itemView.findViewById(R.id.tvBelowMessagesDate);
        }
    }
}
