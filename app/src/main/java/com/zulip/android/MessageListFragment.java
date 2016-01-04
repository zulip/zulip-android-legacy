package com.zulip.android;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class MessageListFragment extends Fragment implements MessageListener {
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated to
     * the activity and potentially other fragments contained in that activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface Listener {
        void onListResume(MessageListFragment f);

        void openCompose(Stream stream, String topic);

        void openCompose(String pmRecipients);

        void openCompose(final MessageType type, String stream, String topic,
                String pmRecipients);
    }

    private static final String PARAM_FILTER = "filter";
    NarrowFilter filter;

    private Listener mListener;

    private View view;
    private ListView listView;
    private View loadIndicatorTop;
    private View loadIndicatorBottom;
    private View bottom_list_spacer;

    public ZulipApp app;

    SparseArray<Message> messageIndex;
    MessageAdapter adapter;
    boolean loadingMessages = true;

    // Whether we've loaded all available messages in that direction
    boolean loadedToTop = false;
    boolean loadedToBottom = false;

    int firstMessageId = -1;
    int lastMessageId = -1;

    boolean paused = false;
    boolean initialized = false;

    public static MessageListFragment newInstance(NarrowFilter filter) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putParcelable(PARAM_FILTER, filter);
        fragment.setArguments(args);
        return fragment;
    }

    public MessageListFragment() {
        app = ZulipApp.get();
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filter = getArguments().getParcelable(PARAM_FILTER);
        }

        messageIndex = new SparseArray<Message>();
        adapter = new MessageAdapter(getActivity(), new ArrayList<Message>());
    }

    public void onPause() {
        super.onPause();
        paused = true;
    }

    public void onResume() {
        super.onResume();
        mListener.onListResume(this);
        paused = false;
    }

    public void onActivityResume() {
        // Only when the activity resumes, not when the fragment is brought to
        // the top
        showLoadIndicatorBottom(true);
        loadingMessages = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_message_list, container,
                false);

        listView = (ListView) view.findViewById(R.id.listview);

        // Load indicator
        View loadTopParent = inflater.inflate(R.layout.list_loading, null);
        View loadBottomParent = inflater.inflate(R.layout.list_loading, null);
        loadIndicatorTop = ((LinearLayout) loadTopParent).getChildAt(0);
        loadIndicatorBottom = ((LinearLayout) loadBottomParent).getChildAt(0);
        listView.addHeaderView(loadTopParent, null, false);
        listView.addFooterView(loadBottomParent, null, false);

        // Spacer
        bottom_list_spacer = new ImageView(getActivity());
        size_bottom_spacer();
        listView.addFooterView(this.bottom_list_spacer);

        listView.setAdapter(adapter);

        // We want blue highlights when you longpress
        listView.setDrawSelectorOnTop(true);

        registerForContextMenu(listView);

        listView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {

                final int near = 6;

                if (!paused && !loadingMessages && firstMessageId > 0
                        && lastMessageId > 0) {
                    if (firstVisibleItem + visibleItemCount > totalItemCount
                            - near) {
                        // At the bottom of the list
                        if (!loadedToBottom) {
                            Log.i("scroll", "Starting request below");
                            loadMoreMessages(LoadPosition.BELOW);
                        }
                    }
                    if (firstVisibleItem < near) {
                        // At the top of the list
                        if (!loadedToTop) {
                            Log.i("scroll", "Starting request above");
                            loadMoreMessages(LoadPosition.ABOVE);
                        }
                    }
                }

            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                try {
                    // Scrolling messages isn't meaningful unless we have
                    // messages to scroll.
                    Message message = ((Message) view.getItemAtPosition(view
                            .getFirstVisiblePosition()));
                    int mID = message.getID();
                    if (filter == null && app.getPointer() < mID) {
                        Log.i("scrolling", "Now at " + mID);
                        (new AsyncPointerUpdate(app)).execute(mID);
                        app.setPointer(mID);
                    }

                    app.markMessageAsRead(message);

                } catch (NullPointerException e) {
                    Log.w("scrolling",
                            "Could not find a location to scroll to!");
                }
            }
        });

        listView.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                try {
                    int mID = (Integer) view.getTag(R.id.messageID);
                    if (app.getPointer() < mID) {
                        Log.i("keyboard", "Now at " + mID);
                        (new AsyncPointerUpdate(app)).execute(mID);
                        app.setPointer(mID);
                    }
                } catch (NullPointerException e) {
                    Log.e("selected", "None, because we couldn't find the tag.");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // pass

            }

        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    void showLoadIndicatorBottom(boolean show) {
        loadIndicatorBottom.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    void showLoadIndicatorTop(boolean show) {
        loadIndicatorTop.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private Message itemFromMenuInfo(ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        // Subtract 1 because it counts the header
        return adapter.getItem(info.position - 1);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Message msg = itemFromMenuInfo(menuInfo);
        if (msg == null) {
            return;
        }
        if (msg.getType().equals(MessageType.STREAM_MESSAGE)) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_stream, menu);
        } else if (msg.getPersonalReplyTo(app).length > 1) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_private, menu);
        } else {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_single_private, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Message message = itemFromMenuInfo((AdapterContextMenuInfo) item
                .getMenuInfo());
        switch (item.getItemId()) {
        case R.id.reply_to_stream:
            mListener.openCompose(message.getStream(), message.getSubject());
            return true;
        case R.id.reply_to_private:
            mListener.openCompose(message.getReplyTo(app));
            return true;
        case R.id.reply_to_sender:
            mListener.openCompose(message.getSender().getEmail());
            return true;
        case R.id.narrow_to_private:
            ((ZulipActivity) getActivity()).doNarrow(new NarrowFilterPM(Arrays
                    .asList(message.getRecipients(app))));
            return true;
        case R.id.narrow_to_stream:
            // TODO This should probably use an interface defining a custom
            // listener.
            ((ZulipActivity) getActivity()).doNarrow(new NarrowFilterStream(
                    message.getStream(), null));
            return true;
        case R.id.narrow_to_subject:
            ((ZulipActivity) getActivity()).doNarrow(new NarrowFilterStream(
                    message.getStream(), message.getSubject()));
            return true;
        case R.id.copy_message:
            copyMessage(message);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    public void onReadyToDisplay(boolean registered) {
        if (initialized && !registered) {
            // Already have state, and already processed any events that came in
            // when resuming the existing queue.
            showLoadIndicatorBottom(false);
            loadingMessages = false;
            Log.i("onReadyToDisplay", "just a resume");
            return;
        }

        adapter.clear();
        messageIndex.clear();

        firstMessageId = -1;
        lastMessageId = -1;

        loadingMessages = true;
        showLoadIndicatorBottom(true);

        fetch();
        initialized = true;
    }

    private void fetch() {
        final AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.execute(app.getPointer(), LoadPosition.INITIAL, 100,
                100, filter);
    }

    private void selectPointer() {
        if (filter != null) {
            Where<Message, Object> filteredWhere;
            try {
                filteredWhere = filter.modWhere(app.getDao(Message.class)
                        .queryBuilder().where());

                filteredWhere.and().le(Message.ID_FIELD, app.getPointer());

                QueryBuilder<Message, Object> closestQuery = app.getDao(
                        Message.class).queryBuilder();

                closestQuery.orderBy(Message.TIMESTAMP_FIELD, false).setWhere(
                        filteredWhere);
                listView.setSelection(adapter.getPosition(closestQuery
                        .queryForFirst()));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            int anc = app.getPointer();
            selectMessage(getMessageById(anc));
        }
    }

    private void size_bottom_spacer() {
        @SuppressWarnings("deprecation")
        // needed for compat with API <13
        int windowHeight = ((WindowManager) app
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getHeight();

        AbsListView.LayoutParams params = new AbsListView.LayoutParams(0, 0);
        params.height = windowHeight / 2;
        this.bottom_list_spacer.setLayoutParams(params);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void copyMessage(Message msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) app
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Zulip Message",
                    msg.getContent()));
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) app
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(msg.getContent());
        }
    }

    public void onNewMessages(Message[] messages) {
        onMessages(messages, LoadPosition.NEW, false, false, false);
    }

    public void onMessages(Message[] messages, LoadPosition pos,
            boolean moreAbove, boolean moreBelow, boolean noFurtherMessages) {

        if (!initialized) {
            return;
        }

        Log.i("onMessages", "Adding " + messages.length + " messages at " + pos);

        // Collect state used to maintain scroll position
        int topPosBefore = listView.getFirstVisiblePosition();
        View topView = listView.getChildAt(0);
        int topOffsetBefore = (topView != null) ? topView.getTop() : 0;
        if (topOffsetBefore >= 0 && !moreAbove && !noFurtherMessages) {
            // If the loading indicator was visible, show a new message in the
            // space it took up. If it was not visible, avoid jumping.
            topOffsetBefore -= loadIndicatorTop.getHeight();
        }
        int addedCount = 0;

        if (pos == LoadPosition.NEW) {
            if (!loadedToBottom) {
                // If we don't have intermediate messages loaded, don't add new
                // messages -- they'll be loaded when we scroll down.
                Log.i("onMessage",
                        "skipping new message " + messages[0].getID() + " "
                                + app.getMaxMessageId());
                return;
            }
        }

        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];

            if (filter != null && !filter.matches(message)) {
                continue;
            }

            if (this.messageIndex.get(message.getID()) != null) {
                // Already have this message.
                Log.i("onMessage", "Already have " + message.getID());
                continue;
            }

            this.messageIndex.append(message.getID(), message);
            Stream stream = message.getStream();

            if (filter == null && stream != null && !stream.getInHomeView()) {
                continue;
            }

            if (pos == LoadPosition.NEW || pos == LoadPosition.BELOW) {
                this.adapter.add(message);
            } else if (pos == LoadPosition.ABOVE || pos == LoadPosition.INITIAL) {
                // TODO: Does this copy the array every time?
                this.adapter.insert(message, addedCount);
                addedCount++;
            }

            if (message.getID() > lastMessageId) {
                lastMessageId = message.getID();
            }

            if (message.getID() < firstMessageId || firstMessageId == -1) {
                firstMessageId = message.getID();
            }
        }

        if (pos == LoadPosition.ABOVE) {
            showLoadIndicatorTop(moreAbove);
            // Restore the position of the top item
            this.listView.setSelectionFromTop(topPosBefore + addedCount,
                    topOffsetBefore);

            if (noFurtherMessages) {
                loadedToTop = true;
            }
        } else if (pos == LoadPosition.BELOW) {
            showLoadIndicatorBottom(moreBelow);

            if (noFurtherMessages || listHasMostRecent()) {
                loadedToBottom = true;
            }
        } else if (pos == LoadPosition.INITIAL) {
            selectPointer();

            showLoadIndicatorTop(moreAbove);
            showLoadIndicatorBottom(moreBelow);

            if (noFurtherMessages || listHasMostRecent()) {
                loadedToBottom = true;
            }
        }

        loadingMessages = moreAbove || moreBelow;
    }

    public void onMessageError(LoadPosition pos) {
        loadingMessages = false;
        // Keep the loading indicator there to indicate that it was not
        // successful
    }

    public void loadMoreMessages(LoadPosition pos) {
        int above = 0;
        int below = 0;
        int around;

        if (pos == LoadPosition.ABOVE) {
            above = 100;
            around = firstMessageId;
            showLoadIndicatorTop(true);
        } else if (pos == LoadPosition.BELOW) {
            below = 100;
            around = lastMessageId;
            showLoadIndicatorBottom(true);
        } else {
            Log.e("loadMoreMessages", "Invalid position");
            return;
        }

        Log.i("loadMoreMessages", "" + around + " " + pos + " " + above + " "
                + below);

        loadingMessages = true;

        AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.execute(around, pos, above, below, filter);
    }

    public Boolean listHasMostRecent() {
        return lastMessageId == app.getMaxMessageId();
    }

    public void selectMessage(final Message message) {
        listView.setSelection(adapter.getPosition(message));
    }

    public Message getMessageById(int id) {
        return this.messageIndex.get(id);
    }
}
