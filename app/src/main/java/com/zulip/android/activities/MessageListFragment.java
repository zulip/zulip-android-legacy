package com.zulip.android.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.zulip.android.models.Person;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.AsyncGetOldMessages;
import com.zulip.android.networking.ZulipAsyncPushTask;
import com.zulip.android.networking.response.EditResponse;
import com.zulip.android.networking.response.RawMessageResponse;
import com.zulip.android.networking.util.DefaultCallback;
import com.zulip.android.util.CommonProgressDialog;
import com.zulip.android.util.Constants;
import com.zulip.android.util.MessageListener;
import com.zulip.android.util.MutedTopics;
import com.zulip.android.util.ZLog;
import com.zulip.android.viewholders.HeaderSpaceItemDecoration;

import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This is a Fragment which holds the recyclerView for displaying the messages
 * initiated and called by {@link ZulipActivity}
 */
public class MessageListFragment extends Fragment implements MessageListener {
    private static final String PARAM_FILTER = "filter";
    public NarrowFilter filter;
    public ZulipApp app;
    RecyclerMessageAdapter adapter;
    TextView emptyTextView;
    private LinearLayoutManager linearLayoutManager;
    private MutedTopics mMutedTopics;
    private Listener mListener;
    private RecyclerView recyclerView;
    private SparseArray<Message> messageIndex;
    private boolean loadingMessages = true;
    // Whether we've loaded all available messages in that direction
    private boolean loadedToTop = false;
    private boolean loadedToBottom = false;
    private int firstMessageId = -1;
    private int lastMessageId = -1;
    // anchor message id used while narrowing to a subject or stream
    private int anchorId = -1;
    private boolean paused = false;
    private boolean initialized = false;
    private List<Message> messageList;

    public MessageListFragment() {
        app = ZulipApp.get();
        mMutedTopics = MutedTopics.get();
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
        messageIndex = new SparseArray<>();
        messageList = new ArrayList<>();
        adapter = new RecyclerMessageAdapter(messageList, getActivity(), (filter != null));
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
        recyclerView.addItemDecoration(new HeaderSpaceItemDecoration(getContext()));
        //as onCreateView is even called when fragment is popped from stack
        //if adapter is null then create new instance else use the previous one
        if (adapter == null) {
            adapter = new RecyclerMessageAdapter(messageList, getActivity(), (filter != null));
        } else {
            //footer is shown when it is resumed, and then it is hidden in setupLists method
            //but now setupLists is not called so hide footer here
            adapter.setFooterShowing(false);
        }
        recyclerView.setAdapter(adapter);
        registerForContextMenu(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                //check if scrolled at last
                mListener.recyclerViewScrolled(linearLayoutManager.findLastCompletelyVisibleItemPosition() == adapter.getItemCount() - 2);
                final int near = 6;
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!paused && !loadingMessages && firstMessageId > 0 && lastMessageId > 0) {
                        int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
                        if (lastVisiblePosition > adapter.getItemCount(false) - near) { // At the bottom of the list
                            Log.i("scroll", "Starting request below");
                            loadMoreMessages(LoadPosition.BELOW);
                        }
                        if (linearLayoutManager.findFirstVisibleItemPosition() < near && !loadedToTop) {
                            // At the top of the list
                            Log.i("scroll", "Starting request above");
                            loadMoreMessages(LoadPosition.ABOVE);
                        }
                    }
                }
            }
        });
        mListener.setLayoutBehaviour(linearLayoutManager, adapter);
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
        // fragment replace transaction can be committed during long press on a message
        int position = adapter.getContextMenuItemSelectedPosition();
        if (position == RecyclerView.NO_POSITION) {
            Toast.makeText(getContext(), R.string.try_again_error_msg, Toast.LENGTH_SHORT).show();
            return false;
        }

        Message message = (Message) adapter.getItem(position);
        switch (item.getItemId()) {
            case R.id.reply_to_stream:
                ((NarrowListener) getActivity()).onNarrowFillSendBox(message, true);
                return true;
            case R.id.reply_to_private:
                ((NarrowListener) getActivity()).onNarrowFillSendBox(message, true);
                return true;
            case R.id.reply_to_sender:
                Person[] senderList = {message.getSender()};
                ((NarrowListener) getActivity()).onNarrowFillSendBoxPrivate(senderList, true);
                return true;
            case R.id.narrow_to_private:
                if (getActivity() instanceof NarrowListener) {
                    ((NarrowListener) getActivity()).onNarrow(new NarrowFilterPM(Arrays.asList(message.getRecipients(app))), message.getId());
                    ((NarrowListener) getActivity()).onNarrowFillSendBox(message, false);
                }
                return true;
            case R.id.narrow_to_stream:
                if (getActivity() instanceof NarrowListener) {
                    ((NarrowListener) getActivity()).onNarrow(new NarrowFilterStream(message.getStream(), null), message.getId());
                    ((NarrowListener) getActivity()).onNarrowFillSendBox(message, false);
                }
                return true;
            case R.id.narrow_to_subject:
                if (getActivity() instanceof NarrowListener) {
                    ((NarrowListener) getActivity()).onNarrow(new NarrowFilterStream(message.getStream(), message.getSubject()), message.getId());
                    ((NarrowListener) getActivity()).onNarrowFillSendBox(message, false);
                }
                return true;
            case R.id.copy_message:
                copyMessage(message);
                return true;
            case R.id.edit_message:
                editMessage(message, adapter.getContextMenuItemSelectedPosition());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void initializeNarrow() {
        adapter.clear();
        messageIndex.clear();

        firstMessageId = -1;
        lastMessageId = -1;

        loadingMessages = true;
        adapter.setFooterShowing(true);
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

        initializeNarrow();
        fetch();
        initialized = true;
    }

    /**
     * Prepare to display narrowed view with {@param messageId} as anchor
     *
     * @param registered is event registered
     * @param messageId  anchor message id
     */
    public void onReadyToDisplay(boolean registered, int messageId) {
        if (initialized && !registered) {
            // Already have state, and already processed any events that came in
            // when resuming the existing queue.
            adapter.setFooterShowing(false);
            loadingMessages = false;
            Log.i("onReadyToDisplay", "just a resume");
            return;
        }

        initializeNarrow();
        fetch(messageId);
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
                if (result.equals("0")) {
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

    /**
     * This function fetches messages using {@link MessageListFragment#filter} and anchors them
     * around {@param messageID}.
     *
     * @param messageID anchor message id
     */
    private void fetch(int messageID) {
        // set the achor for fetching messages as the message clicked
        this.anchorId = messageID;

        final AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result, JSONObject jsonObject) {
                loadingMessages = false;
                adapter.setHeaderShowing(false);
                if (result.equals("0")) {
                    showEmptyView();
                }
            }

            @Override
            public void onTaskFailure(String result) {
                loadingMessages = false;
                adapter.setHeaderShowing(false);
            }
        });
        oldMessagesReq.execute(messageID, LoadPosition.INITIAL, 100,
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
                Message closestMessage = closestQuery.queryForFirst();

                // use anchor message id if message was narrowed
                if (anchorId != -1) {
                    selectMessage(getMessageById(anchorId));
                } else {
                    recyclerView.scrollToPosition(adapter.getItemIndex(closestMessage));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            int anc = app.getPointer();
            selectMessage(getMessageById(anc));
        }
    }

    /**
     * Edit a message passed as parameter
     *
     * @param message Message to be edited
     */
    private void editMessage(final Message message, final int position) {
        boolean isEditingAllowed = app.
                getSettings().
                getBoolean(Constants.IS_EDITING_ALLOWED, Constants.DEFAULT_EDITING_ALLOWED);
        if (!isEditingAllowed) {
            Toast.makeText(getContext(), R.string.editing_message_disabled, Toast.LENGTH_SHORT).show();
            return;
        }
        int maxMessageContentEditLimit = app.
                getSettings().
                getInt(Constants.MAXIMUM_CONTENT_EDIT_LIMIT, Constants.DEFAULT_MAXIMUM_CONTENT_EDIT_LIMIT);
        int timeSinceMessageSend = (int) ((System.currentTimeMillis() - message.getTimestamp().getTime()) / 1000);
        if (timeSinceMessageSend > maxMessageContentEditLimit) {
            Toast.makeText(getContext(), R.string.maximum_time_limit_error, Toast.LENGTH_SHORT).show();
        } else {
            final CommonProgressDialog commonProgressDialog = new CommonProgressDialog(getContext());
            commonProgressDialog.showWithMessage(getString(R.string.fetch_edit_message));
            app.getZulipServices()
                    .fetchRawMessage(message.getID())
                    .enqueue(new DefaultCallback<RawMessageResponse>() {
                        @Override
                        public void onSuccess(Call<RawMessageResponse> call, Response<RawMessageResponse> response) {
                            RawMessageResponse messageResponse = response.body();
                            commonProgressDialog.dismiss();
                            showEditMessageDialog(message, messageResponse.getRawContent(), position);
                        }

                        @Override
                        public void onError(Call<RawMessageResponse> call, Response<RawMessageResponse> response) {
                            RawMessageResponse messageResponse = response.body();
                            if (messageResponse != null) {
                                Toast.makeText(getActivity(), (TextUtils.isEmpty(messageResponse.getMsg())) ? getString(R.string.message_edit_failed) :
                                        messageResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), R.string.message_edit_failed, Toast.LENGTH_SHORT).show();
                            }
                            //If something fails msg will have something to display
                            commonProgressDialog.dismiss();
                        }

                        @Override
                        public void onFailure(Call<RawMessageResponse> call, Throwable t) {
                            super.onFailure(call, t);
                            commonProgressDialog.dismiss();
                            Toast.makeText(getActivity(), R.string.message_edit_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showEditMessageDialog(final Message message, final String rawContent, final int position) {

        final View dialogView = View.inflate(getContext(),
                R.layout.message_edit_dialog, null);
        //Pop up a dialog box with previous message content
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.edit_message)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        final EditText dialogMessageEditText = (EditText) dialogView.findViewById(R.id.message_content);
        dialogMessageEditText.setText(rawContent);

        //Move cursor to end of text
        dialogMessageEditText.setSelection(dialogMessageEditText.getText().length());

        //OK button listener
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            //Show edit option only on if current user send it.
            @Override
            public void onClick(View view) {
                dialog.cancel();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                //Start a progress dialog indicating editing message
                final ProgressDialog progress = new ProgressDialog(getActivity());
                progress.setCancelable(false);
                progress.setMessage(app.getString(R.string.editing_message));
                progress.show();
                final String editedMessageContent =
                        dialogMessageEditText.getText().toString().length() == 0 ? getString(R.string.default_delete_text) : dialogMessageEditText.getText().toString();

                if (editedMessageContent.equals(rawContent)) {
                    Toast.makeText(getActivity(), R.string.no_edit, Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                    return;
                }

                app.getZulipServices()
                        .editMessage(String.valueOf(message.getID()), editedMessageContent)
                        .enqueue(new Callback<EditResponse>() {
                            @Override
                            public void onResponse(Call<EditResponse> call, Response<EditResponse> response) {
                                if (response.isSuccessful()) {
                                    //The message editing work in the list will be done by the AsyncGetEvents#processUpdateMessages
                                    progress.dismiss();
                                    Toast.makeText(getActivity(), R.string.message_edited, Toast.LENGTH_SHORT).show();
                                } else {
                                    progress.dismiss();
                                    Toast.makeText(getActivity(), R.string.message_edit_failed, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<EditResponse> call, Throwable t) {
                                progress.dismiss();
                                ZLog.logException(t);
                                Toast.makeText(getActivity(), R.string.message_edit_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void copyMessage(final Message message) {
        final CommonProgressDialog commonProgressDialog = new CommonProgressDialog(getContext());
        commonProgressDialog.showWithMessage(getString(R.string.copy_message_fetch));
        app.getZulipServices()
                .fetchRawMessage(message.getID())
                .enqueue(new DefaultCallback<RawMessageResponse>() {
                    @Override
                    public void onSuccess(Call<RawMessageResponse> call, Response<RawMessageResponse> response) {
                        RawMessageResponse messageResponse = response.body();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) app
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText("Zulip Message",
                                    messageResponse.getRawContent()));
                        } else {
                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) app
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(messageResponse.getRawContent());
                        }
                        Toast.makeText(getActivity(), R.string.message_copied, Toast.LENGTH_SHORT).show();
                        commonProgressDialog.dismiss();
                    }

                    @Override
                    public void onError(Call<RawMessageResponse> call, Response<RawMessageResponse> response) {
                        RawMessageResponse messageResponse = response.body();
                        if (messageResponse != null) {
                            Toast.makeText(getActivity(), (TextUtils.isEmpty(messageResponse.getMsg())) ? getString(R.string.copy_failed) :
                                    messageResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), R.string.copy_failed, Toast.LENGTH_SHORT).show();
                        }
                        //If something fails msg will have something to display
                        commonProgressDialog.dismiss();
                    }

                    @Override
                    public void onFailure(Call<RawMessageResponse> call, Throwable t) {
                        super.onFailure(call, t);
                        commonProgressDialog.dismiss();
                        Toast.makeText(getActivity(), R.string.copy_failed, Toast.LENGTH_SHORT).show();
                    }
                });
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
        StringBuilder stringBuilder = new StringBuilder();
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
                if (mMutedTopics.isTopicMute(message)) {
                    continue;
                }
            }
            if (filter == null && stream != null && !stream.getInHomeView()) {
                continue;
            }

            if (pos == LoadPosition.NEW || pos == LoadPosition.BELOW) {
                this.adapter.addNewMessage(message);
                messageList.add(message);
            } else if (pos == LoadPosition.ABOVE || pos == LoadPosition.INITIAL) {
                headerParents = (this.adapter.addOldMessage(message, addedCount + headerParents, stringBuilder)) ? headerParents + 1 : headerParents;
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
            /* if we don't account for message headers generated, a scroll jitter is observed
             whenever messages are loaded ABOVE as the original top message's position is not
             retained. We also need to account for the message header of original top message in
             order to retain the correct scroll position.
            */

            // Restore the position of the top item
            // account for generated message headers
            // +1 for the header of top item
            this.recyclerView.scrollToPosition(topPosBefore + addedCount + headerParents + 1);

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
        //check size of messageList
        if (messageList.size() == 0)
            showEmptyView();
        else
            showRecyclerView();
    }

    /**
     * hides TextView with no message
     * show recyclerView
     */
    private void showRecyclerView() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
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

    private void loadMessageId(int id) {
        if (lastMessageId > id) {
            int index = adapter.getItemIndex(id);
            if (index != -1) {
                recyclerView.scrollToPosition(adapter.getItemIndex(id));
                return;
            }
        }
        AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result, JSONObject jsonObject) {
                adapter.setFooterShowing(false);
                loadingMessages = false;
            }

            @Override
            public void onTaskFailure(String result) {
                Toast.makeText(getActivity(), R.string.no_message, Toast.LENGTH_SHORT).show();
                adapter.setFooterShowing(false);
                loadingMessages = false;
            }
        });
        adapter.clear();
        adapter.setFooterShowing(true);
        loadingMessages = true;
        oldMessagesReq.execute(id, LoadPosition.BELOW, 0, 100, filter);
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

        void recyclerViewScrolled(boolean isReachedAtBottom);

        void clearChatBox();

        void setLayoutBehaviour(LinearLayoutManager linearLayoutManager, RecyclerMessageAdapter adapter);

    }

    public void showLatestMessages() {
        if (listHasMostRecent()) {
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        } else {
            loadMessageId(app.getMaxMessageId());
        }
    }

    public boolean scrolledToLastMessage() {
        Object object = adapter.getItem(linearLayoutManager.findLastVisibleItemPosition());
        return object instanceof Message && (((Message) object).getId() >= app.getMaxMessageId() - 2);
    }

    public RecyclerMessageAdapter getAdapter() {
        return this.adapter;
    }
}
