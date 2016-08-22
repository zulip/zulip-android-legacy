package com.zulip.android.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowFilter;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.filters.NarrowFilterStream;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.AsyncGetOldMessages;
import com.zulip.android.networking.ZulipAsyncPushTask;
import com.zulip.android.util.MessageListener;
import com.zulip.android.viewholders.HeaderSpaceItemDecoration;

import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a Fragment which holds the recyclerView for displaying the messages
 * initiated and called by {@link ZulipActivity}
 */
public class MessageListFragment extends Fragment implements MessageListener {
    private static final int PIXEL_OFFSET_MESSAGE_HEADERS = 24;
    private LinearLayoutManager linearLayoutManager;

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated to
     * the activity and potentially other fragments contained in that activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface Listener {
        void onListResume(MessageListFragment f);

        void addToList(Message message);

        void muteTopic(Message message);

        void recyclerViewScrolled();

        void clearChatBox();
    }

    private static final String PARAM_FILTER = "filter";
    public NarrowFilter filter;

    private Listener mListener;
    private RecyclerView recyclerView;
    private View bottom_list_spacer;

    public ZulipApp app;

    private SparseArray<Message> messageIndex;
    RecyclerMessageAdapter adapter;
    private boolean loadingMessages = true;
    // Whether we've loaded all available messages in that direction
    private boolean loadedToTop = false;
    private boolean loadedToBottom = false;

    private List<Message> mutedMessages;
    private int firstMessageId = -1;
    private int lastMessageId = -1;

    TextView emptyTextView;
    private boolean paused = false;
    private boolean initialized = false;
    private List<Message> messageList;

    public MessageListFragment() {
        app = ZulipApp.get();
        // Required empty public constructor
    }

    public static MessageListFragment newInstance(NarrowFilter filter) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putParcelable(PARAM_FILTER, filter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filter = getArguments().getParcelable(PARAM_FILTER);
        }
        mutedMessages = new ArrayList<>();
        messageIndex = new SparseArray<>();
        messageList = new ArrayList<>();
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
        adapter.setFooterShowing(true);
        loadingMessages = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message_list, container,
                false);

        emptyTextView = (TextView) view.findViewById(R.id.emptyList);
        if (filter != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null)
            ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);
        else
            ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_24dp);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new RecyclerMessageAdapter(messageList, getActivity(), (filter == null));
        recyclerView.addItemDecoration(new HeaderSpaceItemDecoration(PIXEL_OFFSET_MESSAGE_HEADERS, getContext()));
        recyclerView.setAdapter(adapter);
        registerForContextMenu(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                mListener.recyclerViewScrolled();
                final int near = 6;
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!paused && !loadingMessages && firstMessageId > 0 && lastMessageId > 0) {
                        int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
                        if (lastVisiblePosition > adapter.getItemCount(false) - near) { // At the bottom of the list
                            Log.i("scroll", "Starting request below");
                            loadMoreMessages(LoadPosition.BELOW);
                        }
                        if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() < near && !loadedToTop) {
                            // At the top of the list
                            Log.i("scroll", "Starting request above");
                            loadMoreMessages(LoadPosition.ABOVE);
                        }
                    }
                }
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
        mListener.clearChatBox();
        mListener = null;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Message message = (Message) adapter.getItem(adapter.getContextMenuItemSelectedPosition());
        switch (item.getItemId()) {
            case R.id.reply_to_stream:
                ((NarrowListener) getActivity()).onNarrowFillSendBox(message, true);
                return true;
            case R.id.reply_to_private:
                ((NarrowListener) getActivity()).onNarrowFillSendBox(message, true);
                return true;
            case R.id.reply_to_sender:
                ((NarrowListener) getActivity()).onNarrowFillSendBox(message, true);
                return true;
            case R.id.narrow_to_private:
                if (getActivity() instanceof NarrowListener) {
                    ((NarrowListener) getActivity()).onNarrow(new NarrowFilterPM(Arrays.asList(message.getRecipients(app))));
                    ((NarrowListener) getActivity()).onNarrowFillSendBox(message, false);
                }
                return true;
            case R.id.narrow_to_stream:
                if (getActivity() instanceof NarrowListener) {
                    ((NarrowListener) getActivity()).onNarrow(new NarrowFilterStream(message.getStream(), null));
                    ((NarrowListener) getActivity()).onNarrowFillSendBox(message, false);
                }
                return true;
            case R.id.narrow_to_subject:
                if (getActivity() instanceof NarrowListener) {
                    ((NarrowListener) getActivity()).onNarrow(new NarrowFilterStream(message.getStream(), message.getSubject()));
                    ((NarrowListener) getActivity()).onNarrowFillSendBox(message, false);
                }
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
            adapter.setFooterShowing(false);
            loadingMessages = false;
            Log.i("onReadyToDisplay", "just a resume");
            return;
        }

        adapter.clear();
        messageIndex.clear();

        firstMessageId = -1;
        lastMessageId = -1;

        loadingMessages = true;
        adapter.setFooterShowing(true);

        fetch();
        initialized = true;
    }

    private void showEmptyView() {
        Log.d("ErrorRecieving", "No Messages found for current list" + ((filter != null) ? ":" + filter.getTitle() : ""));
        recyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
    }

    private void fetch() {
        final AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result, JSONObject jsonObject) {
                loadingMessages = false;
                adapter.setHeaderShowing(false);
                if(result.equals("0")){
                    showEmptyView();
                }
            }

            @Override
            public void onTaskFailure(String result) {
                loadingMessages = false;
                adapter.setHeaderShowing(false);
            }
        });
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

                recyclerView.scrollToPosition(adapter.getItemIndex(closestQuery
                        .queryForFirst()));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            int anc = app.getPointer();
            selectMessage(getMessageById(anc));
        }
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
        int topPosBefore = linearLayoutManager.findFirstVisibleItemPosition();
        int addedCount = 0;
        int headerParents = 0;

        if (pos == LoadPosition.NEW && !loadedToBottom) {
            // If we don't have intermediate messages loaded, don't add new
            // messages -- they'll be loaded when we scroll down.
            Log.i("onMessage",
                    "skipping new message " + messages[0].getID() + " "
                            + app.getMaxMessageId());
            return;
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

            if (stream != null && filter == null) { //Filter muted messages only in homescreen.
                if (app.isTopicMute(message)) {
                    mListener.addToList(message);
                    return;
                }
            }
            if (filter == null && stream != null && !stream.getInHomeView()) {
                continue;
            }

            if (pos == LoadPosition.NEW || pos == LoadPosition.BELOW) {
                this.adapter.addNewMessage(message);
                messageList.add(message);
            } else if (pos == LoadPosition.ABOVE || pos == LoadPosition.INITIAL) {
                headerParents = (this.adapter.addMessage(message, addedCount + headerParents)) ? headerParents + 1 : headerParents;
                messageList.add(addedCount, message);
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
            adapter.setHeaderShowing(moreAbove);
            // Restore the position of the top item
            this.recyclerView.scrollToPosition(topPosBefore + addedCount);

            if (noFurtherMessages) {
                loadedToTop = true;
            }
        } else if (pos == LoadPosition.BELOW) {
            adapter.setFooterShowing(moreBelow);
            loadingMessages = moreBelow;
            if (noFurtherMessages || listHasMostRecent()) {
                loadedToBottom = true;
            }
        } else if (pos == LoadPosition.INITIAL) {
            selectPointer();

            adapter.setHeaderShowing(moreAbove);
            adapter.setFooterShowing(moreBelow);

            if (noFurtherMessages || listHasMostRecent()) {
                loadedToBottom = true;
            }
        }

        loadingMessages = false;
    }

    public void onMessageError(LoadPosition pos) {
        loadingMessages = false;
        // Keep the loading indicator there to indicate that it was not
        // successful
    }

    public void stopRecyclerViewScroll() {
        recyclerView.stopScroll();
    }

    private void loadMoreMessages(final LoadPosition pos) {
        int above = 0;
        int below = 0;
        int around;

        if (pos == LoadPosition.ABOVE) {
            above = 100;
            around = firstMessageId;
            adapter.setHeaderShowing(true);
        } else if (pos == LoadPosition.BELOW) {
            below = 100;
            around = lastMessageId;
            adapter.setFooterShowing(true);
            loadingMessages = true;
        } else {
            Log.e("loadMoreMessages", "Invalid position");
            return;
        }

        Log.i("loadMoreMessages", "" + around + " " + pos + " " + above + " "
                + below);

        loadingMessages = true;

        AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.execute(around, pos, above, below, filter);

        oldMessagesReq.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result, JSONObject jsonObject) {
                adapter.setFooterShowing(false);
                if (pos == LoadPosition.ABOVE) {
                    adapter.setHeaderShowing(false);
                } else if (pos == LoadPosition.BELOW) {
                    adapter.setFooterShowing(false);
                }
                loadingMessages = false;
            }

            @Override
            public void onTaskFailure(String result) {
                Toast.makeText(getActivity(), R.string.no_message, Toast.LENGTH_SHORT).show();
                if (pos == LoadPosition.ABOVE) {
                    adapter.setHeaderShowing(false);
                } else if (pos == LoadPosition.BELOW) {
                    adapter.setFooterShowing(false);
                }
                loadingMessages = false;
            }
        });
    }

    private Boolean listHasMostRecent() {
        return lastMessageId == app.getMaxMessageId();
    }

    private void selectMessage(final Message message) {
        recyclerView.scrollToPosition(adapter.getItemIndex(message));
    }

    private Message getMessageById(int id) {
        return this.messageIndex.get(id);
    }
}
