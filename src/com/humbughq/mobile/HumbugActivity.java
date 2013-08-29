package com.humbughq.mobile;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.humbughq.mobile.HumbugAsyncPushTask.AsyncTaskCompleteListener;

public class HumbugActivity extends Activity {
    ZulipApp app;

    ListView listView;

    SparseArray<Message> messageIndex;
    MessageAdapter adapter;

    public enum LoadPosition {
        ABOVE, BELOW, NEW, INITIAL,
    }

    int firstMessageId = -1;
    int lastMessageId = -1;
    int lastAvailableMessageId = -1;
    boolean loadingMessages = true;

    boolean suspended = false;

    boolean logged_in = false;

    String api_key;

    View bottom_list_spacer;

    HumbugActivity that = this; // self-ref
    SharedPreferences settings;
    String client_id;
    protected int mIDSelected;
    private Menu menu;
    public Person you;
    private View composeView;

    protected HashMap<String, Bitmap> gravatars = new HashMap<String, Bitmap>();

    private AsyncGetEvents event_poll;

    private OnItemClickListener tileClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            try {
                openCompose((Message) parent.getItemAtPosition(position));
            } catch (IndexOutOfBoundsException e) {
                // We can ignore this because its probably before the data
                // has been fetched.
            }

        }

    };

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (ZulipApp) getApplicationContext();
        settings = app.settings;

        if (!app.isLoggedIn()) {
            openLogin();
            return;
        }

        // If we're creating a new Activity, but the app state is still around,
        // re-register anyway to update the view. Won't be necessary once we
        // pull messages from the DB.
        app.clearConnectionState();

        this.onPrepareOptionsMenu(menu);

        this.logged_in = true;
        messageIndex = new SparseArray<Message>();

        setContentView(R.layout.main);
        listView = (ListView) findViewById(R.id.listview);

        this.bottom_list_spacer = new ImageView(this);
        this.size_bottom_spacer();
        listView.addFooterView(this.bottom_list_spacer);

        adapter = new MessageAdapter(this, new ArrayList<Message>());
        listView.setAdapter(adapter);

        // We want blue highlights when you longpress
        listView.setDrawSelectorOnTop(true);

        registerForContextMenu(listView);

        listView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {

                final int near = 6;

                if (!loadingMessages && firstMessageId > 0 && lastMessageId > 0) {
                    if (firstVisibleItem + visibleItemCount > totalItemCount
                            - near) {
                        Log.i("scroll", "at bottom " + loadingMessages + " "
                                + listHasMostRecent() + " " + lastMessageId
                                + " " + lastAvailableMessageId);
                        // At the bottom of the list
                        if (!listHasMostRecent()) {
                            loadMoreMessages(LoadPosition.BELOW);
                        }
                    }
                }

            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                try {
                    // Scrolling messages isn't meaningful unless we have
                    // messages to scroll.
                    int mID = ((Message) view.getItemAtPosition(view
                            .getFirstVisiblePosition())).getID();
                    if (mIDSelected != mID) {
                        Log.i("scrolling", "Now at " + mID);
                        (new AsyncPointerUpdate(that)).execute(mID);
                        mIDSelected = mID;
                    }
                } catch (NullPointerException e) {
                    Log.w("scrolling",
                            "Could not find a location to scroll to!");
                }
            }
        });

        listView.setOnItemClickListener(tileClickListener);
        listView.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                try {
                    int mID = (Integer) view.getTag(R.id.messageID);
                    if (mIDSelected != mID) {
                        Log.i("keyboard", "Now at " + mID);
                        (new AsyncPointerUpdate(that)).execute(mID);
                        mIDSelected = mID;
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

        this.startRequests();
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        /*
         * We want to show a menu only when we're logged in, so this function is
         * called by both Android and our own app when we encounter state
         * changes where we might want to update the menu.
         */
        if (menu == null) {
            // We were called by a function before the menu had been
            // initialised, so we should bail.
            return false;
        }
        this.menu = menu;

        menu.clear();
        if (this.logged_in) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.options, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.compose_stream:
            openCompose(MessageType.STREAM_MESSAGE);
            break;
        case R.id.compose_pm:
            openCompose(MessageType.PRIVATE_MESSAGE);
            break;
        case R.id.refresh:
            Log.w("menu", "Refreshed manually by user. We shouldn't need this.");
            onRefresh();
            break;
        case R.id.logout:
            logout();
            break;
        case R.id.legal:
            openLegal();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Switches the compose window's state to compose a personal message.
     */
    protected void switchToPersonal() {
        EditText recipient = (EditText) composeView
                .findViewById(R.id.composeRecipient);
        EditText subject = (EditText) composeView
                .findViewById(R.id.composeSubject);

        subject.setVisibility(View.GONE);
        recipient.setGravity(Gravity.FILL_HORIZONTAL);
        recipient.setHint(R.string.pm_prompt);
    }

    /**
     * Switches the compose window's state to compose a stream message.
     */
    protected void switchToStream() {
        EditText recipient = (EditText) composeView
                .findViewById(R.id.composeRecipient);
        EditText subject = (EditText) composeView
                .findViewById(R.id.composeSubject);

        subject.setVisibility(View.VISIBLE);
        recipient.setGravity(Gravity.NO_GRAVITY);
        recipient.setHint(R.string.stream);
    }

    protected void openCompose(Message msg) {
        openCompose(msg, null);
    }

    protected void openCompose(MessageType type) {
        openCompose(null, type);
    }

    private void openCompose(Message msg, MessageType type) {
        openCompose(msg, type, false);
    }

    private void openCompose(Message msg, MessageType type, boolean to_sender) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        composeView = inflater.inflate(R.layout.compose, null);
        AlertDialog composeWindow = builder
                .setView(composeView)
                .setTitle("Compose")
                .setPositiveButton(R.string.send,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int id) {
                                EditText recipient = (EditText) composeView
                                        .findViewById(R.id.composeRecipient);

                                EditText subject = (EditText) composeView
                                        .findViewById(R.id.composeSubject);

                                EditText body = (EditText) composeView
                                        .findViewById(R.id.composeText);

                                // If the subject field is hidden, we have a
                                // personal
                                // message.
                                boolean subjectFilledIfRequired = subject
                                        .getVisibility() == View.GONE
                                        || requireFilled(subject, "subject");

                                if (!(requireFilled(recipient, "recipient")
                                        && subjectFilledIfRequired && requireFilled(
                                        body, "message body"))) {
                                    return;

                                }

                                Message msg = new Message(that.app);
                                msg.setSender(that.you);
                                if (subject.getVisibility() == View.GONE) {
                                    msg.setType(MessageType.PRIVATE_MESSAGE);
                                    msg.setRecipient(recipient.getText()
                                            .toString().split(","));
                                } else {
                                    msg.setType(MessageType.STREAM_MESSAGE);
                                    msg.setStream(new Stream(recipient
                                            .getText().toString()));
                                    msg.setSubject(subject.getText().toString());
                                }

                                msg.setContent(body.getText().toString());

                                AsyncSend sender = new AsyncSend(that, msg);
                                sender.execute();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int id) {
                                d.dismiss();
                            }
                        }).create();

        // ***

        EditText recipient = (EditText) composeView
                .findViewById(R.id.composeRecipient);

        EditText subject = (EditText) composeView
                .findViewById(R.id.composeSubject);

        EditText body = (EditText) composeView.findViewById(R.id.composeText);

        if (type == MessageType.STREAM_MESSAGE
                || (msg != null && msg.getType() == MessageType.STREAM_MESSAGE && !to_sender)) {
            this.switchToStream();
        } else {
            this.switchToPersonal();
        }
        if (msg != null) {
            if (msg.getType() == MessageType.STREAM_MESSAGE && !to_sender) {
                recipient.setText(msg.getStream().getName());
                subject.setText(msg.getSubject());
            } else if (msg.getType() == MessageType.PRIVATE_MESSAGE
                    && !to_sender) {
                recipient.setText(msg.getReplyTo());
            } else {
                recipient.setText(msg.getSender().getEmail());
            }
            body.requestFocus();
        } else {
            // Focus the stream, zero out stream/subject
            recipient.requestFocus();
            recipient.setText("");
            subject.setText("");
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);

        composeWindow
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // Hide physical keyboard if present.
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

                    }
                });
        composeWindow.show();

    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void copyMessage(Message msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Zulip Message",
                    msg.getContent()));
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(msg.getContent());
        }
    }

    /**
     * Check if a field is nonempty and mark fields as invalid if they are.
     * 
     * @param field
     *            The field to check
     * @param fieldName
     *            The human-readable name of the field
     * @return Whether the field correctly validated.
     */
    protected boolean requireFilled(EditText field, String fieldName) {
        if (field.getText().toString().equals("")) {
            field.setError("You must specify a " + fieldName);
            field.requestFocus();
            return false;
        }
        return true;

    }

    /**
     * Log the user out of the app, clearing our cache of their credentials.
     */
    private void logout() {
        this.logged_in = false;

        app.logOut();
        this.openLogin();
    }

    /**
     * Switch to the login view.
     */
    protected void openLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    protected void openLegal() {
        Intent i = new Intent(this, LegalActivity.class);
        startActivityForResult(i, 0);
    }

    private void size_bottom_spacer() {
        @SuppressWarnings("deprecation")
        // needed for compat with API <13
        int windowHeight = ((WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getHeight();

        AbsListView.LayoutParams params = new AbsListView.LayoutParams(0, 0);
        params.height = windowHeight / 2;
        this.bottom_list_spacer.setLayoutParams(params);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // our display has changed, lets recalculate the spacer
        this.size_bottom_spacer();
    }

    protected void onPause() {
        super.onPause();
        Log.i("status", "suspend");
        this.suspended = true;
        if (event_poll != null) {
            event_poll.abort();
        }
    }

    protected void onResume() {
        super.onResume();
        Log.i("status", "resume");
        this.suspended = false;
        startRequests();
    }

    protected void onRefresh() {
        super.onResume();

        if (event_poll != null) {
            event_poll.abort();
        }
        app.clearConnectionState();
        startRequests();
    }

    protected void startRequests() {
        Log.i("zulip", "Starting requests");

        if (event_poll != null) {
            event_poll.abort();
        }
        loadingMessages = true;
        event_poll = new AsyncGetEvents(this);
        event_poll.start();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Using menuInfo, determine which menu to show (stream or private)
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Message msg = messageIndex.get((int) info.id);
        if (msg == null) {
            return;
        }
        if (msg.getType().equals(MessageType.STREAM_MESSAGE)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_stream, menu);
        } else if (msg.getPersonalReplyTo().length > 1) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_private, menu);
        } else {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_single_private, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        Message message = messageIndex.get((int) info.id);
        switch (item.getItemId()) {
        case R.id.reply_to_stream:
            openCompose(message, message.getType());
            return true;
        case R.id.reply_to_private:
            openCompose(message, message.getType());
            return true;
        case R.id.reply_to_sender:
            openCompose(message, MessageType.PRIVATE_MESSAGE, true);
            return true;
        case R.id.copy_message:
            copyMessage(message);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    public void onRegister() {
        adapter.clear();
        messageIndex.clear();

        lastAvailableMessageId = app.max_message_id;
        firstMessageId = -1;
        lastMessageId = -1;

        AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.execute(app.pointer, LoadPosition.INITIAL, 100, 100);
        oldMessagesReq.setCallback(new AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result) {
                that.selectMessage(that.getMessageById(app.pointer));
                loadingMessages = false;
            }
        });

    }

    public void onMessages(Message[] messages, LoadPosition pos) {
        Log.i("onMessages", "Adding " + messages.length + " messages at " + pos);

        if (pos == LoadPosition.NEW) {
            // listHasMostRecent check needs to occur before updating
            // lastAvailableMessageId
            boolean hasMostRecent = listHasMostRecent();
            lastAvailableMessageId = messages[messages.length - 1].getID();
            if (!hasMostRecent) {
                // If we don't have intermediate messages loaded, don't add new
                // messages -- they'll be loaded when we scroll down.
                Log.i("onMessage",
                        "skipping new message " + messages[0].getID() + " "
                                + lastAvailableMessageId);
                return;
            }
        }

        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];

            if (this.messageIndex.get(message.getID()) != null) {
                // Already have this message.
                Log.i("onMessage", "Already have " + message.getID());
                continue;
            }

            this.messageIndex.append(message.getID(), message);
            Stream stream = message.getStream();

            if (stream == null || stream.getInHomeView()) {
                if (pos == LoadPosition.NEW || pos == LoadPosition.BELOW) {
                    this.adapter.add(message);
                } else if (pos == LoadPosition.ABOVE
                        || pos == LoadPosition.INITIAL) {
                    Log.i("onMessages", "Inserting at " + i);
                    this.adapter.insert(message, i);
                }

                if (message.getID() > lastMessageId) {
                    lastMessageId = message.getID();
                }

                if (message.getID() < firstMessageId || firstMessageId == -1) {
                    firstMessageId = message.getID();
                }
            }
        }

    }

    public Boolean listHasMostRecent() {
        return lastMessageId == lastAvailableMessageId;
    }

    public void loadMoreMessages(LoadPosition pos) {
        int above = 0;
        int below = 0;
        int around;

        if (pos == LoadPosition.ABOVE) {
            above = 100;
            around = firstMessageId;
        } else if (pos == LoadPosition.BELOW) {
            below = 100;
            around = lastMessageId;
        } else {
            Log.e("loadMoreMessages", "Invalid position");
            return;
        }

        Log.i("loadMoreMessages", "" + around + " " + pos + " " + above + " "
                + below);

        loadingMessages = true;

        AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);
        oldMessagesReq.execute(around, pos, above, below);
        oldMessagesReq.setCallback(new AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result) {
                loadingMessages = false;
            }
        });
    }

    public void selectMessage(final Message message) {
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(adapter.getPosition(message));
            }
        });
    }

    public Message getMessageById(int id) {
        return this.messageIndex.get(id);
    }
}
