package com.zulip.android.activities;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff.Mode;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.zulip.android.database.DatabaseHelper;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.filters.NarrowFilter;
import com.zulip.android.filters.NarrowFilterAllPMs;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.filters.NarrowFilterSearch;
import com.zulip.android.filters.NarrowFilterStream;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.gcm.Notifications;
import com.zulip.android.models.Person;
import com.zulip.android.models.Presence;
import com.zulip.android.models.PresenceType;
import com.zulip.android.R;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.AsyncSend;
import com.zulip.android.util.ZLog;
import com.zulip.android.ZulipApp;
import com.zulip.android.gcm.GcmBroadcastReceiver;
import com.zulip.android.networking.AsyncGetEvents;
import com.zulip.android.networking.AsyncStatusUpdate;
import com.zulip.android.networking.ZulipAsyncPushTask;

import org.json.JSONObject;

public class ZulipActivity extends FragmentActivity implements
        MessageListFragment.Listener, NarrowListener {

    public static final String NARROW = "narrow";
    public static final String PARAMS = "params";
    ZulipApp app;
    List<Message> mutedTopics;

    boolean suspended = false;
    boolean logged_in = false;

    ZulipActivity that = this; // self-ref
    SharedPreferences settings;
    String client_id;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private Menu menu;

    private HashMap<String, Bitmap> gravatars = new HashMap<>();

    private AsyncGetEvents event_poll;

    private Handler statusUpdateHandler;

    MessageListFragment currentList;
    MessageListFragment narrowedList;
    MessageListFragment homeList;

    AutoCompleteTextView streamActv;
    AutoCompleteTextView topicActv;
    EditText messageEt;
    private TextView textView;
    private ImageView sendBtn;
    private ImageView togglePrivateStreamBtn;
    Notifications notifications;
    SimpleCursorAdapter streamActvAdapter;
    SimpleCursorAdapter subjectActvAdapter;
    SimpleCursorAdapter emailActvAdapter;

    private BroadcastReceiver onGcmMessage = new BroadcastReceiver() {
        public void onReceive(Context contenxt, Intent intent) {
            // Block the event before it propagates to show a notification.
            // TODO: could be smarter and only block the event if the message is
            // in the narrow.
            Log.i("GCM", "Dropping a push because the activity is active");
            abortBroadcast();
        }
    };

    // Intent Extra constants
    public enum Flag {
        RESET_DATABASE,
    }

    private SimpleCursorAdapter.ViewBinder streamBinder = new SimpleCursorAdapter.ViewBinder() {

        @Override
        public boolean setViewValue(View arg0, Cursor arg1, int arg2) {
            switch (arg0.getId()) {
                case R.id.name:
                    TextView name = (TextView) arg0;
                    name.setText(arg1.getString(arg2));
                    //Change color in the drawer if this stream is inHomeView only.
                    if (!Stream.getByName(app, arg1.getString(arg2)).getInHomeView())
                        name.setTextColor(ContextCompat.getColor(ZulipActivity.this, android.R.color.tertiary_text_light));
                    else name.setTextColor(ContextCompat.getColor(ZulipActivity.this, android.R.color.primary_text_light));
                    return true;
                case R.id.stream_dot:
                    // Set the color of the (currently white) dot
                    arg0.setVisibility(View.VISIBLE);
                    arg0.getBackground().setColorFilter(arg1.getInt(arg2),
                            Mode.MULTIPLY);
                    return true;
                default:
                    break;
            }
            return false;
        }

    };

    public HashMap<String, Bitmap> getGravatars() {
        return gravatars;
    }

    private SimpleCursorAdapter.ViewBinder peopleBinder = new SimpleCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int i) {
            switch (view.getId()) {
                case R.id.name:
                    TextView name = (TextView) view;
                    name.setText(cursor.getString(i));
                    return true;
                case R.id.stream_dot:
                    String email = cursor.getString(i);
                    if (app == null || email == null) {
                        view.setVisibility(View.INVISIBLE);
                    } else {
                        Presence presence = app.presences.get(email);
                        if (presence == null) {
                            view.setVisibility(View.INVISIBLE);
                        } else {
                            PresenceType status = presence.getStatus();
                            long age = presence.getAge();
                            if (age > 2 * 60) {
                                view.setVisibility(View.VISIBLE);
                                view.setBackgroundResource(R.drawable.presence_inactive);
                            } else if (PresenceType.ACTIVE == status) {
                                view.setVisibility(View.VISIBLE);
                                view.setBackgroundResource(R.drawable.presence_active);
                            } else if (PresenceType.IDLE == status) {
                                view.setVisibility(View.VISIBLE);
                                view.setBackgroundResource(R.drawable.presence_away);
                            } else {
                                view.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    protected RefreshableCursorAdapter streamsAdapter;
    protected RefreshableCursorAdapter peopleAdapter;

    @Override
    public void addToList(Message message) {
        mutedTopics.add(message);
    }

    @Override
    public void muteTopic(Message message) {
        app.muteTopic(message);
        for (int i = homeList.adapter.getCount() - 1; i >= 0; i--) {
            if (homeList.adapter.getItem(i).getStream() != null
                    && homeList.adapter.getItem(i).getStream().getId() == message.getStream().getId()
                    && homeList.adapter.getItem(i).getSubject().equals(message.getSubject())) {
                mutedTopics.add(homeList.adapter.getItem(i));
                homeList.adapter.remove(homeList.adapter.getItem(i));
            }
        }
        homeList.adapter.notifyDataSetChanged();
    }

    public RefreshableCursorAdapter getPeopleAdapter() {
        return peopleAdapter;
    }

    public RefreshableCursorAdapter getStreamsAdapter() {
        return streamsAdapter;
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (ZulipApp) getApplicationContext();
        settings = app.getSettings();

        processParams();

        if (!app.isLoggedIn()) {
            openLogin();
            return;
        }

        notifications = new Notifications(this);
        notifications.register();

        this.onPrepareOptionsMenu(menu);

        this.logged_in = true;

        setContentView(R.layout.main);
        streamActv = (AutoCompleteTextView) findViewById(R.id.stream_actv);
        topicActv = (AutoCompleteTextView) findViewById(R.id.topic_actv);
        messageEt = (EditText) findViewById(R.id.message_et);
        textView = (TextView) findViewById(R.id.textView);
        sendBtn = (ImageView) findViewById(R.id.send_btn);
        togglePrivateStreamBtn = (ImageView) findViewById(R.id.togglePrivateStream_btn);
        mutedTopics = new ArrayList<>();
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
                        .where().eq(Stream.SUBSCRIBED_FIELD, true).queryRaw()
                        .closeableIterator().getRawResults()).getRawCursor();
            }
        };

        // row number which is used to differentiate the 'All private messages'
        // row from the people
        final int allPeopleId = -1;
        Callable<Cursor> peopleGenerator = new Callable<Cursor>() {

            @Override
            public Cursor call() throws Exception {
                // TODO Auto-generated method stub
                List<Person> people = app.getDao(Person.class).queryBuilder()
                        .where().eq(Person.ISBOT_FIELD, false).and()
                        .eq(Person.ISACTIVE_FIELD, true).query();

                Person.sortByPresence(app, people);

                String[] columnsWithPresence = new String[]{"_id",
                        Person.EMAIL_FIELD, Person.NAME_FIELD};

                MatrixCursor sortedPeopleCursor = new MatrixCursor(
                        columnsWithPresence);
                for (Person person : people) {
                    Object[] row = new Object[]{person.getId(), person.getEmail(),
                            person.getName()};
                    sortedPeopleCursor.addRow(row);
                }

                // add private messages row
                MatrixCursor allPrivateMessages = new MatrixCursor(
                        sortedPeopleCursor.getColumnNames());
                Object[] row = new Object[]{allPeopleId, "",
                        "All private messages"};

                allPrivateMessages.addRow(row);

                return new MergeCursor(new Cursor[]{
                        allPrivateMessages, sortedPeopleCursor});

            }

        };
        try {
            this.streamsAdapter = new RefreshableCursorAdapter(
                    this.getApplicationContext(), R.layout.stream_tile,
                    streamsGenerator.call(), streamsGenerator, new String[]{
                    Stream.NAME_FIELD, Stream.COLOR_FIELD}, new int[]{
                    R.id.name, R.id.stream_dot}, 0);
            streamsAdapter.setViewBinder(streamBinder);
            streamsDrawer.setAdapter(streamsAdapter);

            this.peopleAdapter = new RefreshableCursorAdapter(
                    this.getApplicationContext(), R.layout.stream_tile,
                    peopleGenerator.call(), peopleGenerator, new String[]{
                    Person.NAME_FIELD, Person.EMAIL_FIELD}, new int[]{
                    R.id.name, R.id.stream_dot}, 0);
            peopleAdapter.setViewBinder(peopleBinder);

            peopleDrawer.setAdapter(peopleAdapter);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            ZLog.logException(e);
        }

        streamsDrawer.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO: is there a way to get the Stream from the adapter
                // without re-querying it?
                Stream stream = Stream.getById(app, (int) id);
                narrow(stream);
                streamActv.setText(stream.getName());
                topicActv.setText("");
            }
        });

        peopleDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (id == allPeopleId) {
                    doNarrow(new NarrowFilterAllPMs(app.getYou()));
                } else {
                    narrow_pm_with(Person.getById(app, (int) id));
                }
            }
        });

        // send status update and check again every couple minutes
        statusUpdateHandler = new Handler();
        Runnable statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                AsyncStatusUpdate task = new AsyncStatusUpdate(
                        ZulipActivity.this);
                task.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
                    @Override
                    public void onTaskComplete(String result, JSONObject object) {
                        peopleAdapter.refresh();
                    }

                    @Override
                    public void onTaskFailure(String result) {

                    }
                });
                task.execute();
                statusUpdateHandler.postDelayed(this, 2 * 60 * 1000);
            }
        };
        statusUpdateHandler.post(statusUpdateRunnable);

        if (android.os.Build.VERSION.SDK_INT >= 11 && getActionBar() != null) {
            // the AB is unavailable when invoked from JUnit
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        homeList = MessageListFragment.newInstance(null);
        pushListFragment(homeList, null);
        streamActv = (AutoCompleteTextView) findViewById(R.id.stream_actv);
        topicActv = (AutoCompleteTextView) findViewById(R.id.topic_actv);
        messageEt = (EditText) findViewById(R.id.message_et);
        textView = (TextView) findViewById(R.id.textView);
        sendBtn = (ImageView) findViewById(R.id.send_btn);
        togglePrivateStreamBtn = (ImageView) findViewById(R.id.togglePrivateStream_btn);
        togglePrivateStreamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView();
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        composeStatus = (LinearLayout) findViewById(R.id.composeStatus);
        setUpAdapter();
        streamActv.setAdapter(streamActvAdapter);
        topicActv.setAdapter(subjectActvAdapter);
    }

    private void sendMessage() {

        if (isCurrentModeStream()) {
            if (TextUtils.isEmpty(streamActv.getText().toString())) {
                streamActv.setError(getString(R.string.stream_error));
                streamActv.requestFocus();
                return;
            } else {
                try {
                    Cursor streamCursor = makeStreamCursor(streamActv.getText().toString());
                    if (streamCursor.getCount() == 0) {
                        streamActv.setError(getString(R.string.stream_not_exists));
                        streamActv.requestFocus();
                        return;
                    }
                } catch (SQLException e) {
                    Log.e("SQLException", "SQL not correct", e);
                }
            }
            if (TextUtils.isEmpty(topicActv.getText().toString())) {
                topicActv.setError(getString(R.string.subject_error));
                topicActv.requestFocus();
                return;
            }
        } else {
            if (TextUtils.isEmpty(topicActv.getText().toString())) {
                topicActv.setError(getString(R.string.person_error));
                topicActv.requestFocus();
                return;
            }
        }

        if (TextUtils.isEmpty(messageEt.getText().toString())) {
            messageEt.setError(getString(R.string.no_message_error));
            messageEt.requestFocus();
            return;
        }
        sendingMessage(true);
        MessageType messageType = (isCurrentModeStream()) ? MessageType.STREAM_MESSAGE : MessageType.PRIVATE_MESSAGE;
        Message msg = new Message(app);
        msg.setSender(app.getYou());

        if (messageType == MessageType.STREAM_MESSAGE) {
            msg.setType(messageType);
            msg.setStream(new Stream(streamActv.getText().toString()));
            msg.setSubject(topicActv.getText().toString());
        } else if (messageType == MessageType.PRIVATE_MESSAGE) {
            msg.setType(messageType);
            msg.setRecipient(topicActv.getText().toString().split(","));
        }
        msg.setContent(messageEt.getText().toString());
        AsyncSend sender = new AsyncSend(that, msg);
        sender.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
            public void onTaskComplete(String result, JSONObject jsonObject) {
                Toast.makeText(ZulipActivity.this, R.string.message_sent, Toast.LENGTH_SHORT).show();
                messageEt.setText("");
                sendingMessage(false);
            }
            public void onTaskFailure(String result) {
                Log.d("onTaskFailure", "Result: " + result);
                Toast.makeText(ZulipActivity.this, R.string.message_error, Toast.LENGTH_SHORT).show();
                sendingMessage(false);
            }
        });
        sender.execute();
    }

    private void sendingMessage(boolean isSending) {
        streamActv.setEnabled(!isSending);
        textView.setEnabled(!isSending);
        messageEt.setEnabled(!isSending);
        topicActv.setEnabled(!isSending);
        sendBtn.setEnabled(!isSending);
        togglePrivateStreamBtn.setEnabled(!isSending);
        if (isSending)
            composeStatus.setVisibility(View.VISIBLE);
        else
            composeStatus.setVisibility(View.GONE);
    }

    LinearLayout composeStatus;

    public void setUpAdapter() {
        streamActvAdapter = new SimpleCursorAdapter(
                that, R.layout.stream_tile, null,
                new String[]{Stream.NAME_FIELD},
                new int[]{R.id.name}, 0);
        streamActvAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                int index = cursor.getColumnIndex(Stream.NAME_FIELD);
                return cursor.getString(index);
            }
        });
        streamActvAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                try {
                    return makeStreamCursor(charSequence);
                } catch (SQLException e) {
                    Log.e("SQLException", "SQL not correct", e);
                    return null;
                }
            }
        });
        subjectActvAdapter = new SimpleCursorAdapter(
                that, R.layout.stream_tile, null,
                new String[]{Message.SUBJECT_FIELD},
                new int[]{R.id.name}, 0);
        subjectActvAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                int index = cursor.getColumnIndex(Message.SUBJECT_FIELD);
                return cursor.getString(index);
            }
        });
        subjectActvAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                try {
                    return makeSubjectCursor(streamActv.getText().toString(), charSequence);
                } catch (SQLException e) {
                    Log.e("SQLException", "SQL not correct", e);
                    return null;
                }
            }
        });

        emailActvAdapter = new SimpleCursorAdapter(
                that, R.layout.stream_tile, null,
                new String[]{Person.EMAIL_FIELD},
                new int[]{R.id.name}, 0);
        emailActvAdapter
                .setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
                    @Override
                    public CharSequence convertToString(Cursor cursor) {
                        String text = topicActv.getText().toString();
                        String prefix;
                        int lastIndex = text.lastIndexOf(",");
                        if (lastIndex != -1) {
                            prefix = text.substring(0, lastIndex + 1);
                        } else {
                            prefix = "";
                        }
                        int index = cursor.getColumnIndex(Person.EMAIL_FIELD);
                        return prefix + cursor.getString(index);
                    }
                });
        emailActvAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                try {
                    return makePeopleCursor(charSequence);
                } catch (SQLException e) {
                    Log.e("SQLException", "SQL not correct", e);
                    return null;
                }
            }
        });

        sendingMessage(false);
    }

    private Cursor makeStreamCursor(CharSequence streamName)
            throws SQLException {
        if (streamName == null) {
            streamName = "";
        }

        return ((AndroidDatabaseResults) app
                .getDao(Stream.class)
                .queryRaw(
                        "SELECT rowid _id, * FROM streams WHERE "
                                + Stream.SUBSCRIBED_FIELD + " = 1 AND "
                                + Stream.NAME_FIELD
                                + " LIKE ? ESCAPE '\\' ORDER BY "
                                + Stream.NAME_FIELD + " COLLATE NOCASE",
                        DatabaseHelper.likeEscape(streamName.toString()) + "%")
                .closeableIterator().getRawResults()).getRawCursor();
    }

    private Cursor makeSubjectCursor(CharSequence stream, CharSequence subject)
            throws SQLException {
        if (subject == null) {
            subject = "";
        }
        if (stream == null) {
            stream = "";
        }

        AndroidDatabaseResults results = (AndroidDatabaseResults) app
                .getDao(Message.class)
                .queryRaw(
                        "SELECT DISTINCT "
                                + Message.SUBJECT_FIELD
                                + ", 1 AS _id FROM messages JOIN streams ON streams."
                                + Stream.ID_FIELD + " = messages."
                                + Message.STREAM_FIELD + " WHERE "
                                + Message.SUBJECT_FIELD
                                + " LIKE ? ESCAPE '\\' AND "
                                + Stream.NAME_FIELD + " = ? ORDER BY "
                                + Message.SUBJECT_FIELD + " COLLATE NOCASE",
                        DatabaseHelper.likeEscape(subject.toString()) + "%",
                        stream.toString()).closeableIterator().getRawResults();
        return results.getRawCursor();
    }

    private Cursor makePeopleCursor(CharSequence email) throws SQLException {
        if (email == null) {
            email = "";
        }
        String[] pieces = TextUtils.split(email.toString(), ",");
        String piece;
        if (pieces.length == 0) {
            piece = "";
        } else {
            piece = pieces[pieces.length - 1].trim();
        }
        return ((AndroidDatabaseResults) app
                .getDao(Person.class)
                .queryRaw(
                        "SELECT rowid _id, * FROM people WHERE "
                                + Person.ISBOT_FIELD + " = 0 AND "
                                + Person.ISACTIVE_FIELD + " = 1 AND "
                                + Person.EMAIL_FIELD
                                + " LIKE ? ESCAPE '\\' ORDER BY "
                                + Person.NAME_FIELD + " COLLATE NOCASE",
                        DatabaseHelper.likeEscape(piece) + "%")
                .closeableIterator().getRawResults()).getRawCursor();
    }
    public void switchToStream() {
        removeEditTextErrors();
        if (!isCurrentModeStream()) switchView();
    }

    public void switchToPrivate() {
        removeEditTextErrors();
        if (isCurrentModeStream()) switchView();
    }

    public boolean isCurrentModeStream() {
        //The TextView is VISIBLE which means currently send to stream is on.
        return (textView.getVisibility() == View.VISIBLE);
    }

    public void removeEditTextErrors() {
        streamActv.setError(null);
        topicActv.setError(null);
        messageEt.setError(null);
    }

    public void switchView() {
        if (isCurrentModeStream()) { //Person
            togglePrivateStreamBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_action_bullhorn));
            tempStreamSave = topicActv.getText().toString();
            topicActv.setText(null);
            topicActv.setHint(R.string.hint_person);
            topicActv.setAdapter(emailActvAdapter);
            streamActv.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
        } else { //Stream
            topicActv.setText(tempStreamSave);
            togglePrivateStreamBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_action_person));
            streamActv.setEnabled(true);
            topicActv.setHint(R.string.hint_subject);
            streamActv.setHint(R.string.hint_stream);
            streamActv.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            topicActv.setVisibility(View.VISIBLE);
            streamActv.setAdapter(streamActvAdapter);
            topicActv.setAdapter(subjectActvAdapter);
        }
    }
    String tempStreamSave = null;

    @Override
    public void clearChatBox() {
        if (messageEt != null) {
            if (TextUtils.isEmpty(messageEt.getText())) {
                topicActv.setText("");
                streamActv.setText("");
            }
        }
    }
    public void onBackPressed() {
        if (narrowedList != null) {
            narrowedList = null;
            getSupportFragmentManager().popBackStack(NARROW,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            super.onBackPressed();
        }
    }

    private void pushListFragment(MessageListFragment list, String back) {
        currentList = list;
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.list_fragment_container, list);
        if (back != null) {
            transaction.addToBackStack(back);
        }
        transaction.commit();
        getSupportFragmentManager().executePendingTransactions();
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
                    Log.e(PARAMS, "Invalid app-specific intent specified.", e);
                    continue;
                }
            } else {
                continue;
            }
            switch (param) {
                case RESET_DATABASE:
                    Log.i(PARAMS, "Resetting the database...");
                    app.resetDatabase();
                    Log.i(PARAMS, "Database deleted successfully.");
                    this.finish();
                    break;
                default:
                    break;
            }
        }
    }

    protected void narrow(final Stream stream) {
        doNarrow(new NarrowFilterStream(stream, null));
    }

    protected void narrow_pm_with(final Person person) {
        doNarrow(new NarrowFilterPM(Arrays.asList(app.getYou(), person)));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onListResume(MessageListFragment list) {
        currentList = list;

        NarrowFilter filter = list.filter;
        if (filter == null) {
            setupTitleBar(getString(R.string.app_name), null);
            this.drawerToggle.setDrawerIndicatorEnabled(true);
        } else {
            setupTitleBar(filter.getTitle(), filter.getSubtitle());
            this.drawerToggle.setDrawerIndicatorEnabled(false);
        }

        this.drawerLayout.closeDrawers();
    }

    private void setupTitleBar(String title, String subtitle) {
        if (android.os.Build.VERSION.SDK_INT >= 11 && getActionBar() != null) {
            if (title != null) getActionBar().setTitle(title);
            getActionBar().setSubtitle(subtitle);
        }
    }

    public void doNarrow(NarrowFilter filter) {
        narrowedList = MessageListFragment.newInstance(filter);
        // Push to the back stack if we are not already narrowed
        pushListFragment(narrowedList, NARROW);
        narrowedList.onReadyToDisplay(true);
    }

    @Override
    public void onNarrowFillSendBox(Message message) {
        if(message.getType() == MessageType.PRIVATE_MESSAGE){
            switchToPrivate();
            topicActv.setText(message.getReplyTo(app));
            messageEt.requestFocus();
        } else {
            switchToStream();
            streamActv.setText(message.getStream().getName());
            topicActv.setText(message.getSubject());
            if ("".equals(message.getSubject())) topicActv.requestFocus();
            else messageEt.requestFocus();
        }
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
    public void onNarrow(NarrowFilter narrowFilter) {
        // TODO: check if already narrowed to this particular stream/subject
        doNarrow(narrowFilter);
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

        if (this.logged_in && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Get the SearchView and set the searchable configuration
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            final MenuItem searchMenuItem = menu.findItem(R.id.search);
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            // Assumes current activity is the searchable activity
            searchView.setSearchableInfo(searchManager
                    .getSearchableInfo(getComponentName()));

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String s) {
                            doNarrow(new NarrowFilterSearch(s));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                searchMenuItem.collapseActionView();
                            }
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String s) {
                            return false;
                        }
                    });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            // Close the right drawer if we opened the left one
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        }

        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack(NARROW,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
            case R.id.search:
                // show a pop up dialog only if gingerbread or under
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Search Zulip");
                    final EditText editText = new EditText(this);
                    builder.setView(editText);

                    builder.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface, int i) {
                                    String query = editText.getText().toString();
                                    doNarrow(new NarrowFilterSearch(query));
                                }
                            });
                    builder.show();
                }
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

    /**
     * Log the user out of the app, clearing our cache of their credentials.
     */
    private void logout() {
        this.logged_in = false;

        notifications.logOut(new Runnable() {
            public void run() {
                app.logOut();
                openLogin();
            }
        });
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

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // our display has changed, lets recalculate the spacer
        // this.size_bottom_spacer();

        drawerToggle.onConfigurationChanged(newConfig);
    }

    protected void onPause() {
        super.onPause();
        Log.i("status", "suspend");
        this.suspended = true;

        unregisterReceiver(onGcmMessage);

        if (event_poll != null) {
            event_poll.abort();
            event_poll = null;
        }

        if (statusUpdateHandler != null) {
            statusUpdateHandler.removeMessages(0);
        }
    }

    protected void onResume() {
        super.onResume();
        Log.i("status", "resume");
        this.suspended = false;

        // Set up the BroadcastReceiver to trap GCM messages so notifications
        // don't show while in the app
        IntentFilter filter = new IntentFilter(GcmBroadcastReceiver.BROADCAST);
        filter.setPriority(2);
        registerReceiver(onGcmMessage, filter);

        homeList.onActivityResume();
        if (narrowedList != null) {
            narrowedList.onActivityResume();
        }
        startRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusUpdateHandler != null) {
            statusUpdateHandler.removeMessages(0);
        }
    }

    protected void onRefresh() {
        super.onResume();

        if (event_poll != null) {
            event_poll.abort();
            event_poll = null;
        }
        app.clearConnectionState();
        app.resetDatabase();
        app.setEmail(app.getYou().getEmail());
        startRequests();
    }

    protected void startRequests() {
        Log.i("zulip", "Starting requests");

        if (event_poll != null) {
            event_poll.abort();
        }

        event_poll = new AsyncGetEvents(this);
        event_poll.start();

    }

    public void onReadyToDisplay(boolean registered) {
        homeList.onReadyToDisplay(registered);
        if (narrowedList != null) {
            narrowedList.onReadyToDisplay(registered);
        }
    }

    public void onNewMessages(Message[] messages) {
        homeList.onNewMessages(messages);
        if (narrowedList != null) {
            narrowedList.onNewMessages(messages);
        }
    }
}
