package com.humbughq.mobile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff.Mode;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.humbughq.mobile.HumbugAsyncPushTask.AsyncTaskCompleteListener;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class HumbugActivity extends FragmentActivity {
    ZulipApp app;

    ListView listView;

    // Intent Extra constants
    public enum Flag {
        RESET_DATABASE,
    }

    SparseArray<Message> messageIndex;
    MessageAdapter adapter;
    MessageAdapter narrowedAdapter;

    public enum LoadPosition {
        ABOVE, BELOW, NEW, INITIAL,
    }

    int firstMessageId = -1;
    int lastMessageId = -1;
    boolean loadingMessages = true;

    boolean suspended = false;

    boolean logged_in = false;

    String api_key;

    View bottom_list_spacer;

    HumbugActivity that = this; // self-ref
    SharedPreferences settings;
    String client_id;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private Menu menu;
    public Person you;

    protected HashMap<String, Bitmap> gravatars = new HashMap<String, Bitmap>();

    private AsyncGetEvents event_poll;

    private ListView narrowedListView;

    private OnItemClickListener tileClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            try {
                Message m = (Message) parent.getItemAtPosition(position);
                openCompose(m.getType(), m.getStream().getName(),
                        m.getSubject(), m.getReplyTo(app));
            } catch (IndexOutOfBoundsException e) {
                // We can ignore this because its probably before the data
                // has been fetched.
            }

        }

    };

    private SimpleCursorAdapter.ViewBinder streamBinder = new SimpleCursorAdapter.ViewBinder() {

        @Override
        public boolean setViewValue(View arg0, Cursor arg1, int arg2) {
            switch (arg0.getId()) {
            case R.id.name:
                TextView name = (TextView) arg0;
                name.setText(arg1.getString(arg2));
                return true;
            case R.id.stream_dot:
                // Set the color of the (currently white) dot
                arg0.setVisibility(View.VISIBLE);
                arg0.getBackground().setColorFilter(arg1.getInt(arg2),
                        Mode.MULTIPLY);
                return true;
            }
            return false;
        }

    };

    private NarrowFilter narrowFilter;

    protected RefreshableCursorAdapter streamsAdapter;

    protected RefreshableCursorAdapter peopleAdapter;

    View loadIndicatorTop;
    View loadIndicatorBottom;

    protected MessageRange currentRange;

    /** Called when the activity is first created. */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (ZulipApp) getApplicationContext();
        settings = app.settings;

        processParams();

        if (!app.isLoggedIn()) {
            openLogin();
            return;
        }

        this.onPrepareOptionsMenu(menu);

        this.logged_in = true;
        messageIndex = new SparseArray<Message>();

        setContentView(R.layout.main);
        listView = (ListView) findViewById(R.id.listview);
        loadIndicatorTop = getLayoutInflater().inflate(R.layout.list_loading,
                null);
        loadIndicatorBottom = getLayoutInflater().inflate(
                R.layout.list_loading, null);
        listView.addHeaderView(loadIndicatorTop, null, false);
        listView.addFooterView(loadIndicatorBottom, null, false);
        showLoadIndicatorTop(true);

        narrowedListView = (ListView) findViewById(R.id.narrowed_listview);

        this.bottom_list_spacer = new ImageView(this);
        this.size_bottom_spacer();
        listView.addFooterView(this.bottom_list_spacer);
        narrowedListView.addFooterView(this.bottom_list_spacer);

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
                                + " " + app.getMaxMessageId());
                        // At the bottom of the list
                        if (!listHasMostRecent()) {
                            loadMoreMessages(LoadPosition.BELOW);
                        }
                    }
                    if (firstVisibleItem < near) {
                        Log.i("scroll", "at top" + firstVisibleItem + " "
                                + visibleItemCount + " " + totalItemCount);
                        loadMoreMessages(LoadPosition.ABOVE);
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
                    if (app.getPointer() < mID) {
                        Log.i("scrolling", "Now at " + mID);
                        (new AsyncPointerUpdate(that)).execute(mID);
                        app.setPointer(mID);
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
                    if (app.getPointer() < mID) {
                        Log.i("keyboard", "Now at " + mID);
                        (new AsyncPointerUpdate(that)).execute(mID);
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

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.streams_open,
                R.string.streams_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                // pass
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                // pass
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        ListView streamsDrawer = (ListView) findViewById(R.id.streams_drawer);
        ListView peopleDrawer = (ListView) findViewById(R.id.people_drawer);

        Callable<Cursor> streamsGenerator = new Callable<Cursor>() {
            @Override
            public Cursor call() throws Exception {
                return ((AndroidDatabaseResults) app.getDao(Stream.class)
                        .queryBuilder().selectRaw("rowid _id", "*")
                        .orderByRaw(Stream.NAME_FIELD + " COLLATE NOCASE")
                        .queryRaw().closeableIterator().getRawResults())
                        .getRawCursor();
            }
        };

        Callable<Cursor> peopleGenerator = new Callable<Cursor>() {

            @Override
            public Cursor call() throws Exception {
                // TODO Auto-generated method stub
                return ((AndroidDatabaseResults) app.getDao(Person.class)
                        .queryBuilder().selectRaw("rowid _id", "*")
                        .orderByRaw(Person.NAME_FIELD + " COLLATE NOCASE")
                        .where().eq(Person.ISBOT_FIELD, false).queryRaw()
                        .closeableIterator().getRawResults()).getRawCursor();
            }

        };
        try {
            this.streamsAdapter = new RefreshableCursorAdapter(
                    this.getApplicationContext(), R.layout.stream_tile,
                    streamsGenerator.call(), streamsGenerator, new String[] {
                            Stream.NAME_FIELD, Stream.COLOR_FIELD }, new int[] {
                            R.id.name, R.id.stream_dot }, 0);
            streamsAdapter.setViewBinder(streamBinder);
            streamsDrawer.setAdapter(streamsAdapter);

            this.peopleAdapter = new RefreshableCursorAdapter(
                    this.getApplicationContext(), R.layout.stream_tile,
                    peopleGenerator.call(), peopleGenerator,
                    new String[] { Person.NAME_FIELD },
                    new int[] { R.id.name }, 0);

            peopleDrawer.setAdapter(peopleAdapter);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        streamsDrawer.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // TODO: is there a way to get the Stream from the adapter
                // without re-querying it?
                narrow(Stream.getById(app, (int) id));
            }
        });

        peopleDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                narrow_pm_with(Person.getById(app, (int) id));
            }
        });

        if (getActionBar() != null) {
            // the AB is unavailable when invoked from JUnit
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
    }

    void showLoadIndicatorBottom(boolean show) {
        loadIndicatorBottom.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    void showLoadIndicatorTop(boolean show) {
        loadIndicatorTop.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void processParams() {
        Bundle params = getIntent().getExtras();
        if (params == null)
            return;
        for (String unprocessedParam : params.keySet()) {
            Flag param;
            if (unprocessedParam.contains(getBaseContext().getPackageName())) {
                try {
                    param = Flag.valueOf(unprocessedParam
                            .substring(getBaseContext().getPackageName()
                                    .length() + 1));
                } catch (IllegalArgumentException e) {
                    Log.e("params", "Invalid app-specific intent specified.", e);
                    continue;
                }
            } else {
                continue;
            }
            switch (param) {
            case RESET_DATABASE:
                Log.i("params", "Resetting the database...");
                boolean result = app.resetDatabase();
                Log.i("params", "Database deleted successfully.");
                this.finish();
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (narrowFilter != null) {
            doUnnarrow();
        } else {
            super.onBackPressed();
        }
    }

    protected void narrow(final Stream stream) {
        try {
            doNarrow(new NarrowFilterStream(stream));
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void narrow_pm_with(final Person person) {
        try {
            doNarrow(new NarrowFilterPM(person));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doNarrow(NarrowFilter filter) throws SQLException {
        Where<Message, Object> filteredWhere = filter.modWhere(app
                .getDao(Message.class).queryBuilder().where());
        List<Message> messages = filteredWhere.query();

        this.narrowedAdapter = new MessageAdapter(this, messages);
        this.narrowedListView.setAdapter(this.narrowedAdapter);
        this.narrowFilter = filter;

        filteredWhere.and().le(Message.ID_FIELD, app.getPointer());

        QueryBuilder<Message, Object> closestQuery = app.getDao(Message.class)
                .queryBuilder();

        closestQuery.orderBy(Message.TIMESTAMP_FIELD, false).setWhere(
                filteredWhere);
        this.narrowedListView.setSelection(this.narrowedAdapter
                .getPosition(closestQuery.queryForFirst()));
        this.listView.setVisibility(View.GONE);
        this.narrowedListView.setVisibility(View.VISIBLE);

        String title = filter.getTitle();
        if (title != null) {
            getActionBar().setTitle(title);
        }
        getActionBar().setSubtitle(filter.getSubtitle());
        this.narrowedListView.setOnItemClickListener(tileClickListener);
        this.drawerToggle.setDrawerIndicatorEnabled(false);
        this.drawerLayout.closeDrawers();

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doUnnarrow() {
        this.narrowedAdapter.clear();
        this.narrowedListView.setVisibility(View.GONE);
        this.listView.setVisibility(View.VISIBLE);
        this.drawerToggle.setDrawerIndicatorEnabled(true);
        getActionBar().setTitle("Zulip");
        getActionBar().setSubtitle(null);
        this.narrowFilter = null;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
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
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            // Close the right drawer if we opened the left one
            drawerLayout.closeDrawer(Gravity.RIGHT);
            return true;
        }

        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            doUnnarrow();
            break;
        case R.id.compose_stream:
            String stream = null;
            if (narrowFilter != null) {
                stream = narrowFilter.getComposeStream().getName();
            }
            openCompose(MessageType.STREAM_MESSAGE, stream, null, null);
            break;
        case R.id.compose_pm:
            String recipient = null;
            if (narrowFilter != null) {
                recipient = narrowFilter.getComposePMRecipient();
            }
            openCompose(MessageType.PRIVATE_MESSAGE, null, null, recipient);
            break;
        case R.id.refresh:
            Log.w("menu", "Refreshed manually by user. We shouldn't need this.");
            onRefresh();
            ((RefreshableCursorAdapter) ((ListView) findViewById(R.id.streams_drawer))
                    .getAdapter()).refresh();
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

    protected void openCompose(MessageType type) {
        openCompose(type, null, null, null);
    }

    protected void openCompose(Stream stream, String topic) {
        openCompose(MessageType.STREAM_MESSAGE, stream.getName(), topic, null);
    }

    protected void openCompose(String pmRecipients) {
        openCompose(MessageType.PRIVATE_MESSAGE, null, null, pmRecipients);
    }

    private void openCompose(final MessageType type, String stream,
            String topic, String pmRecipients) {

        FragmentManager fm = getSupportFragmentManager();
        ComposeDialog dialog = ComposeDialog.newInstance(type, stream, topic,
                pmRecipients);
        dialog.show(fm, "fragment_compose");
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

        drawerToggle.onConfigurationChanged(newConfig);
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
        showLoadIndicatorTop(true);
        event_poll = new AsyncGetEvents(this);
        event_poll.start();
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
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_stream, menu);
        } else if (msg.getPersonalReplyTo(app).length > 1) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_private, menu);
        } else {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_single_private, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Message message = itemFromMenuInfo((AdapterContextMenuInfo) item
                .getMenuInfo());
        switch (item.getItemId()) {
        case R.id.reply_to_stream:
            openCompose(message.getStream(), message.getSubject());
            return true;
        case R.id.reply_to_private:
            openCompose(message.getReplyTo(app));
            return true;
        case R.id.reply_to_sender:
            openCompose(message.getSender().getEmail());
            return true;
        case R.id.copy_message:
            copyMessage(message);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    public void onReadyToDisplay() {
        adapter.clear();
        messageIndex.clear();

        firstMessageId = -1;
        lastMessageId = -1;

        that.populateCurrentRange();
        final AsyncGetOldMessages oldMessagesReq = new AsyncGetOldMessages(this);

        oldMessagesReq
                .execute(app.getPointer(), LoadPosition.INITIAL, 100, 100);
        oldMessagesReq.setCallback(new AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result) {
                int anc = app.getPointer();
                that.selectMessage(that.getMessageById(anc));
                loadingMessages = false;
                showLoadIndicatorTop(false);
            }

            public void onTaskFailure(String result) {

            }
        });

    }

    public void onMessages(Message[] messages, LoadPosition pos) {
        Log.i("onMessages", "Adding " + messages.length + " messages at " + pos);

        // Collect state used to maintain scroll position
        int topPosBefore = listView.getFirstVisiblePosition();
        View topView = listView.getChildAt(0);
        int topOffsetBefore = (topView != null) ? topView.getTop() : 0;
        if (topOffsetBefore >= 0) {
            // If the loading indicator was visible, show a new message in the
            // space it took up. If it was not visible, avoid jumping.
            topOffsetBefore -= loadIndicatorTop.getHeight();
        }
        int addedCount = 0;

        if (pos == LoadPosition.NEW) {
            // listHasMostRecent check needs to occur before updating
            // lastAvailableMessageId
            boolean hasMostRecent = listHasMostRecent();
            app.setMaxMessageId(messages[messages.length - 1].getID());
            if (!hasMostRecent) {
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

                    if (this.narrowFilter != null) {
                        // For some reason calling adapter.add above shows the
                        // main message list. Lets explicitly re-hide it here.
                        this.listView.setVisibility(View.GONE);

                        if (this.narrowFilter.matches(message)) {
                            this.narrowedAdapter.add(message);
                        }
                    }
                } else if (pos == LoadPosition.ABOVE
                        || pos == LoadPosition.INITIAL) {
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
        }

        if (pos == LoadPosition.ABOVE) {
            showLoadIndicatorTop(false);
            Log.i("Header",
                    loadIndicatorTop.getTop() + " "
                            + loadIndicatorTop.getHeight() + " "
                            + topOffsetBefore);
            // Restore the position of the top item
            this.listView.setSelectionFromTop(topPosBefore + addedCount,
                    topOffsetBefore);
        } else if (pos == LoadPosition.BELOW) {
            showLoadIndicatorBottom(false);
        }
    }

    public Boolean listHasMostRecent() {
        return lastMessageId == app.getMaxMessageId();
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
        oldMessagesReq.execute(around, pos, above, below);
        oldMessagesReq.setCallback(new AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result) {
                loadingMessages = false;
            }

            public void onTaskFailure(String result) {
                loadingMessages = false;
            }
        });
    }

    public void selectMessage(final Message message) {
        listView.setSelection(adapter.getPosition(message));
    }

    public Message getMessageById(int id) {
        return this.messageIndex.get(id);
    }

    public void populateCurrentRange() {
        Dao<MessageRange, Integer> messageRangeDao = app
                .getDao(MessageRange.class);
        this.currentRange = MessageRange.getRangeContaining(app.getPointer(),
                messageRangeDao);
        if (this.currentRange == null) {
            this.currentRange = new MessageRange(app.getPointer(),
                    app.getPointer());
            // Does not get saved until we actually have messages here
        }
    }
}
