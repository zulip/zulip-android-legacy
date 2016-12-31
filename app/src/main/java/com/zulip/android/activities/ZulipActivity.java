package com.zulip.android.activities;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.zulip.android.BuildConfig;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.database.DatabaseHelper;
import com.zulip.android.filters.NarrowFilter;
import com.zulip.android.filters.NarrowFilterAllPMs;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.filters.NarrowFilterSearch;
import com.zulip.android.filters.NarrowFilterStream;
import com.zulip.android.filters.NarrowFilterToday;
import com.zulip.android.filters.NarrowListener;
import com.zulip.android.gcm.GcmBroadcastReceiver;
import com.zulip.android.gcm.Notifications;
import com.zulip.android.models.Emoji;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.models.Person;
import com.zulip.android.models.Presence;
import com.zulip.android.models.PresenceType;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.AsyncGetEvents;
import com.zulip.android.networking.AsyncSend;
import com.zulip.android.networking.AsyncStatusUpdate;
import com.zulip.android.networking.ZulipAsyncPushTask;
import com.zulip.android.networking.response.UploadResponse;
import com.zulip.android.util.AnimationHelper;
import com.zulip.android.util.FilePathHelper;
import com.zulip.android.util.MutedTopics;
import com.zulip.android.util.SwipeRemoveLinearLayout;
import com.zulip.android.util.UrlHelper;
import com.zulip.android.util.ZLog;

import org.json.JSONObject;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The main Activity responsible for holding the {@link MessageListFragment} which has the list to the
 * messages
 * */
public class ZulipActivity extends BaseActivity implements
        MessageListFragment.Listener, NarrowListener, SwipeRemoveLinearLayout.leftToRightSwipeListener {

    private static final String NARROW = "narrow";
    private static final String PARAMS = "params";
    //At these many letters the emoji/person hint will not show now on
    private static final int MAX_THRESOLD_EMOJI_HINT = 5;
    //At these many letters the emoji/person hint starts to show up
    private static final int MIN_THRESOLD_EMOJI_HINT = 1;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1;
    private ZulipApp app;

    private boolean logged_in = false;
    private boolean backPressedOnce = false;

    private ZulipActivity that = this; // self-ref

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
    private SwipeRemoveLinearLayout chatBox;
    FloatingActionButton fab;
    private CountDownTimer fabHidder;
    private boolean isTextFieldFocused = false;
    private static final int HIDE_FAB_AFTER_SEC = 5;

    private HashMap<String, Bitmap> gravatars = new HashMap<>();

    private AsyncGetEvents event_poll;

    private Handler statusUpdateHandler;
    private Runnable statusUpdateRunnable;

    public MessageListFragment currentList;
    private MessageListFragment narrowedList;
    private MessageListFragment homeList;

    private AutoCompleteTextView streamActv;
    private AutoCompleteTextView topicActv;
    private AutoCompleteTextView messageEt;
    private TextView textView;
    private ImageView sendBtn;
    private ImageView togglePrivateStreamBtn;
    private Notifications notifications;
    private SimpleCursorAdapter streamActvAdapter;
    private SimpleCursorAdapter subjectActvAdapter;
    private SimpleCursorAdapter emailActvAdapter;
    private AppBarLayout appBarLayout;

    private MutedTopics mMutedTopics;

    private BroadcastReceiver onGcmMessage = new BroadcastReceiver() {
        public void onReceive(Context contenxt, Intent intent) {
            // Block the event before it propagates to show a notification.
            // TODO: could be smarter and only block the event if the message is
            // in the narrow.
            Log.i("GCM", "Dropping a push because the activity is active");
            abortBroadcast();
        }
    };
    private ExpandableStreamDrawerAdapter streamsDrawerAdapter;
    private Uri mImageUri;

    @Override
    public void removeChatBox(boolean animToRight) {
        AnimationHelper.hideViewX(chatBox, animToRight);
    }

    // Intent Extra constants
    public enum Flag {
        RESET_DATABASE,
    }

    private EditText etSearchPeople;
    private ImageView ivSearchPeopleCancel;
    private EditText etSearchStream;
    private ImageView ivSearchStreamCancel;
    private ListView peopleDrawer;
    // row number which is used to differentiate the 'All private messages'
    // row from the people
    final int allPeopleId = -1;

    //
    private String streamSearchFilterKeyword="";

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

    private RefreshableCursorAdapter peopleAdapter;

    @Override
    public void recyclerViewScrolled() {
            /* in this method we check if the messageEt is empty or not
            if messageEt is not empty, it means that the user has typed something in the chatBox and that the chatBox should be open
            in spite of scrolling */
        if(chatBox.getVisibility()== View.VISIBLE && !isCurrentModeStream()){
            //if messageEt is empty in private msg mode, then the chatBox can disappear on scrolling else it will stay
            if(messageEt.getText().toString().equals(""))
            {
                displayChatBox(false);
                displayFAB(true);

            }
        }

        else if(chatBox.getVisibility()==View.VISIBLE && isCurrentModeStream()){
            //check if messageEt is empty in stream msg mode, then the chatBox can disappear on scrolling else it will disappear
            if(messageEt.getText().toString().equals("") && topicActv.getText().toString().equals(""))
            {
                displayChatBox(false);
                displayFAB(true);

            }
        }
        /*check if stream edittext, topic edittext and messageEt edittext is empty in a general msg mode(i.e. when the floating
        button is pressed by user). If all fields are empty, then on scrolling the chatBox will disappear else not  */
        else if (chatBox.getVisibility() == View.VISIBLE && streamActv.getText().toString().equals("") && topicActv.getText().toString().equals("") && messageEt.getText().toString().equals("")) {
            displayChatBox(false);
            displayFAB(true);
        }

    }

    public RefreshableCursorAdapter getPeopleAdapter() {
        return peopleAdapter;
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
        processParams();

        if (!app.isLoggedIn()) {
            openLogin();
            return;
        }

        if (mMutedTopics == null) {
            mMutedTopics = MutedTopics.get();
        }

        this.logged_in = true;
        notifications = new Notifications(this);
        notifications.register();
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_24dp);
        streamActv = (AutoCompleteTextView) findViewById(R.id.stream_actv);
        topicActv = (AutoCompleteTextView) findViewById(R.id.topic_actv);
        messageEt = (AutoCompleteTextView) findViewById(R.id.message_et);
        textView = (TextView) findViewById(R.id.textView);
        sendBtn = (ImageView) findViewById(R.id.send_btn);
        appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        etSearchPeople = (EditText)findViewById(R.id.people_drawer_search);
        ivSearchPeopleCancel = (ImageView)findViewById(R.id.iv_people__search_cancel_button);
        onTextChangeOfPeopleSearchEditText();
        ivSearchPeopleCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set default people list
               resetPeopleSearch();
            }
        });
        etSearchStream = (EditText)findViewById(R.id.stream_drawer_search);
        ivSearchStreamCancel = (ImageView)findViewById(R.id.iv_stream_search_cancel_button);
        onTextChangeOfStreamSearchEditText();
        ivSearchStreamCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set default stream list
               resetStreamSearch();
            }
        });
        app.setZulipActivity(this);
        togglePrivateStreamBtn = (ImageView) findViewById(R.id.togglePrivateStream_btn);
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
                try {
                    streamsDrawerAdapter.changeCursor(getSteamCursorGenerator().call());
                } catch (Exception e) {
                    ZLog.logException(e);
                }
                streamsDrawerAdapter.notifyDataSetChanged();
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);


         peopleDrawer = (ListView) findViewById(R.id.people_drawer);

        //set up people list
        setUpPeopleList();

        peopleDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                resetPeopleSearch();
                if (id == allPeopleId) {
                    doNarrow(new NarrowFilterAllPMs(app.getYou()));
                } else {
                    narrow_pm_with(Person.getById(app, (int) id));
                }
            }
        });

        // send status update and check again every couple minutes
        statusUpdateHandler = new Handler();
        statusUpdateRunnable = new Runnable() {
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
        homeList = MessageListFragment.newInstance(null);
        pushListFragment(homeList, null);
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
        checkAndSetupStreamsDrawer();

        setupFab();
        View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focus) {
                isTextFieldFocused = focus;
            }
        };
        messageEt.setOnFocusChangeListener(focusChangeListener);
        topicActv.setOnFocusChangeListener(focusChangeListener);
        streamActv.setOnFocusChangeListener(focusChangeListener);

        SimpleCursorAdapter combinedAdapter = new SimpleCursorAdapter(
                that, R.layout.emoji_tile, null,
                new String[]{Emoji.NAME_FIELD, Emoji.NAME_FIELD},
                new int[]{R.id.emojiImageView, R.id.nameTV}, 0);

        combinedAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                //TODO - columnIndex is 6 for Person table and columnIndex is 1 for Emoji table, Confirm this will be perfect to distinguish between these two tables! It seems alphabetical ordering of columns!
                boolean personTable = !(columnIndex == 1);
                String name = cursor.getString(cursor.getColumnIndex(Emoji.NAME_FIELD));
                switch (view.getId()) {
                    case R.id.emojiImageView:
                        if (personTable) {
                            view.setVisibility(View.GONE);
                        } else {
                            try {
                                Drawable drawable = Drawable.createFromStream(getApplicationContext().getAssets().open("emoji/" + name),
                                        "emoji/" + name);
                                ((ImageView) view).setImageDrawable(drawable);
                            } catch (Exception e) {
                                ZLog.logException(e);
                            }
                        }
                        return true;
                    case R.id.nameTV:
                        ((TextView) view).setText(name);
                        return true;
                }
                if (BuildConfig.DEBUG)
                    ZLog.logException(new RuntimeException(getResources().getResourceName(view.getId()) + " - this view not binded!"));
                return false;
            }
        });
        combinedAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                if (cursor == null) return messageEt.getText();
                int index = cursor.getColumnIndex(Emoji.NAME_FIELD);
                String name = cursor.getString(index);
                String currText = messageEt.getText().toString();
                int numberOfColumns = cursor.getColumnCount();
                int last = (numberOfColumns > 2) ? currText.lastIndexOf("@") : currText.lastIndexOf(":");
                return TextUtils.substring(currText, 0, last) + ((numberOfColumns > 2) ? "@**" + name + "**" : ":" + name.replace(".png", "") + ":");
            }
        });
        combinedAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                if (charSequence == null) return null;
                int length = charSequence.length();
                int personLength = charSequence.toString().lastIndexOf("@");
                int smileyLength = charSequence.toString().lastIndexOf(":");
                if (length - 1 > Math.max(personLength, smileyLength) + MAX_THRESOLD_EMOJI_HINT
                        || length - Math.max(personLength, smileyLength) - 1 < MIN_THRESOLD_EMOJI_HINT
                        || (personLength + smileyLength == -2))
                    return null;
                try {
                    if (personLength > smileyLength) {
                        return makePeopleNameCursor(charSequence.subSequence(personLength + 1, length));
                    } else {
                        return makeEmojiCursor(charSequence.subSequence(smileyLength + 1, length));
                    }
                } catch (SQLException e) {
                    Log.e("SQLException", "SQL not correct", e);
                    return null;
                }
            }
        });
        messageEt.setAdapter(combinedAdapter);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                // Handle single image being sent
                handleSentImage(intent);
            }
        }
    }

    /**
     * Change peopleAdapter cursor to default
     * Clear text of etSearchPeople
     * Remove focus of etSearchPeople
     */
    private void resetPeopleSearch() {
        try {
            peopleAdapter.changeCursor(getPeopleCursorGenerator().call());
        } catch (Exception e) {
            ZLog.logException(e);
        }
        //set search editText text empty
        etSearchPeople.setText("");
        //hide soft keyboard
        hideSoftKeyBoard();
        //remove focus
        etSearchPeople.clearFocus();
    }

    /**
     * Hide soft keyboard
     */
    private void hideSoftKeyBoard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void onTextChangeOfStreamSearchEditText() {
        etSearchStream.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    streamsDrawerAdapter.changeCursor(getSteamCursorGenerator().call());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private Callable<Cursor> getSteamCursorGenerator() {

        Callable<Cursor> steamCursorGenerator =  new Callable<Cursor>() {
            @Override
            public Cursor call() throws Exception {
                int pointer = app.getPointer();
                String query="SELECT s.id as _id,  s.name, s.color, count(case when m.id > " + pointer + " or m." + Message.MESSAGE_READ_FIELD
                        + " = 0 then 1 end) as " + ExpandableStreamDrawerAdapter.UNREAD_TABLE_NAME
                        + " FROM streams as s LEFT JOIN messages as m ON s.id=m.stream ";
                if (!etSearchStream.getText().toString().equals("") && !etSearchStream.getText().toString().isEmpty())
                {
                    //append where clause
                    query+=" WHERE s.name LIKE '%"+ etSearchStream.getText().toString()  + "%'";
                    //set visibility of this image false
                    ivSearchStreamCancel.setVisibility(View.VISIBLE);
                }else
                {
                    //set visibility of this image false
                    ivSearchStreamCancel.setVisibility(View.GONE);
                }
                //append group by
                query+= " group by s.name order by s.name COLLATE NOCASE";

                return ((AndroidDatabaseResults) app.getDao(Stream.class).queryRaw(query).closeableIterator().getRawResults()).getRawCursor();
            }
        };
        return steamCursorGenerator;
    }

    private void setUpPeopleList() {
        try {
            this.peopleAdapter = new RefreshableCursorAdapter(
                    this.getApplicationContext(), R.layout.stream_tile,
                    getPeopleCursorGenerator().call(), getPeopleCursorGenerator(), new String[]{
                    Person.NAME_FIELD, Person.EMAIL_FIELD}, new int[]{
                    R.id.name, R.id.stream_dot}, 0);
            peopleAdapter.setViewBinder(peopleBinder);

            peopleDrawer.setAdapter(peopleAdapter);
        } catch (SQLException e) {
            ZLog.logException(e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            ZLog.logException(e);
        }
    }

    private void onTextChangeOfPeopleSearchEditText() {
        etSearchPeople.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    peopleAdapter.changeCursor(getPeopleCursorGenerator().call());
                } catch (Exception e) {
                    ZLog.logException(e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private Callable<Cursor> getPeopleCursorGenerator() {
        Callable<Cursor> peopleGenerator = new Callable<Cursor>() {

            @Override
            public Cursor call() throws Exception {
                // TODO Auto-generated method stub
                List<Person> people;
                if (etSearchPeople.getText().toString().equals("") || etSearchPeople.getText().toString().isEmpty()) {
                    people = app.getDao(Person.class).queryBuilder()
                            .where().eq(Person.ISBOT_FIELD, false).and()
                            .eq(Person.ISACTIVE_FIELD, true).query();
                    //set visibility of this image false
                    ivSearchPeopleCancel.setVisibility(View.GONE);
                }else
                {
                    people = app.getDao(Person.class).queryBuilder()
                            .where().eq(Person.ISBOT_FIELD, false).and()
                            .like(Person.NAME_FIELD,"%"+etSearchPeople.getText().toString()+"%").and()
                            .eq(Person.ISACTIVE_FIELD, true).query();
                    //set visibility of this image false
                    ivSearchPeopleCancel.setVisibility(View.VISIBLE);
                }

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
        return peopleGenerator;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Get action and MIME type of intent
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                // Handle single image being sent
                handleSentImage(intent);
            }
        }
    }

    /**
     * Function invoked when a user shares an image with the zulip app
     * @param intent passed to the activity with action SEND
     */
    @SuppressLint("InlinedApi")
    private void handleSentImage(Intent intent) {
        mImageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (mImageUri != null) {
            // check if user has granted read external storage permission
            // for Android 6.0 or higher
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // we need to request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_READ_CONTACTS);
            } else {
                // permission already granted
                // start with file upload
                startFileUpload();
            }
        } else {
            Toast.makeText(this, R.string.cannot_find_image, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PERMISSION_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    // start with file upload
                    startFileUpload();
                } else {
                    // permission denied
                    Toast.makeText(this, R.string.cannot_upload_image, Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }

    /**
     * Helper function to update UI to indicate image is being uploaded and call
     * {@link ZulipActivity#uploadFile(File)} to upload the image.
     */
    private void startFileUpload() {
        // Update UI to indicate image is being loaded
        // hide fab and display chatbox
        displayFAB(false);
        displayChatBox(true);
        String loadingMsg = getResources().getString(R.string.uploading_message);
        sendingMessage(true, loadingMsg);

        File file = null;
        if (FilePathHelper.isLegacy(mImageUri)) {
            file = FilePathHelper.getTempFileFromContentUri(this, mImageUri);
        } else {
            // get actual file path
            String imageFilePath = FilePathHelper.getPath(this, mImageUri);
            if (imageFilePath != null) {
                file = new File(imageFilePath);
            } else if ("content".equalsIgnoreCase(mImageUri.getScheme())) {
                file = FilePathHelper.getTempFileFromContentUri(this, mImageUri);
            }
        }

        if (file == null) {
            Toast.makeText(this, R.string.invalid_image, Toast.LENGTH_SHORT).show();
            return;
        }
        // upload the file asynchronously to the server
        uploadFile(file);
    }

    /**
     * Function to upload file asynchronously to the server using retrofit callback
     * upload {@link com.zulip.android.service.ZulipServices#upload(MultipartBody.Part)}
     * @param file on local storage
     */
    private void uploadFile(File file) {

        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(MediaType.parse("multipart/form-data"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("picture", file.getName(), requestFile);

        final String loadingMsg = getResources().getString(R.string.uploading_message);

        // finally, execute the request
        // create upload service client
        Call<UploadResponse> call = ((ZulipApp) getApplicationContext()).getZulipServices().upload(body);
        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call,
                                   Response<UploadResponse> response) {
                if (response.isSuccessful()) {
                    String filePathOnServer = "";
                        UploadResponse uploadResponse = response.body();
                        filePathOnServer = uploadResponse.getUri();
                    if (!filePathOnServer.equals("")) {
                        // remove loading message from the screen
                        sendingMessage(false, loadingMsg);

                        // print message to compose box
                        messageEt.append(" " + UrlHelper.addHost(filePathOnServer));
                    } else {
                        // remove loading message from the screen
                        sendingMessage(false, loadingMsg);
                        Toast.makeText(ZulipActivity.this, R.string.failed_to_upload, Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    // remove loading message from the screen
                    sendingMessage(false, loadingMsg);
                    Toast.makeText(ZulipActivity.this, R.string.failed_to_upload, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                // remove loading message from the screen
                sendingMessage(false, loadingMsg);
                ZLog.logException(t);
            }
        });
    }

    /**
     * Returns a cursor for the combinedAdapter used to suggest Emoji when ':' is typed in the {@link #messageEt}
     * @param emoji A string to search in the existing database
     */
    private Cursor makeEmojiCursor(CharSequence emoji)
            throws SQLException {
        if (emoji == null) {
            emoji = "";
        }
        return ((AndroidDatabaseResults) app
                .getDao(Emoji.class)
                .queryRaw("SELECT  rowid _id,name FROM emoji WHERE name LIKE '%" + emoji + "%'")
                .closeableIterator().getRawResults()).getRawCursor();
    }

    private Cursor makePeopleNameCursor(CharSequence name) throws SQLException {
        if (name == null) {
            name = "";
        }
        return ((AndroidDatabaseResults) app
                .getDao(Person.class)
                .queryRaw(
                        "SELECT rowid _id, * FROM people WHERE "
                                + Person.ISBOT_FIELD + " = 0 AND "
                                + Person.ISACTIVE_FIELD + " = 1 AND "
                                + Person.NAME_FIELD
                                + " LIKE ? ESCAPE '\\' ORDER BY "
                                + Person.NAME_FIELD + " COLLATE NOCASE",
                        DatabaseHelper.likeEscape(name.toString()) + "%")
                .closeableIterator().getRawResults()).getRawCursor();
    }

    private void setupFab() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        chatBox = (SwipeRemoveLinearLayout) findViewById(R.id.messageBoxContainer);
        chatBox.registerToSwipeEvents(this);
        fabHidder = new CountDownTimer(HIDE_FAB_AFTER_SEC * 1000, HIDE_FAB_AFTER_SEC * 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (!isTextFieldFocused) {
                    displayFAB(true);
                    displayChatBox(false);
                } else {
                    start();
                }
            }
        };
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentList.stopRecyclerViewScroll();
                displayChatBox(true);
                displayFAB(false);
                fabHidder.start();
            }
        });
    }

    private void displayChatBox(boolean show) {
        if (show) {
            showView(chatBox);
        } else {
            hideView(chatBox);
        }
    }

    private void displayFAB(boolean show) {
        if (show) {
            showView(fab);
        } else {
            hideView(fab);
        }
    }

    @SuppressLint("NewApi")
    public void hideView(final View view) {
        ViewPropertyAnimator animator = view.animate()
                .translationY((view instanceof AppBarLayout) ? -1 * view.getHeight() : view.getHeight())
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(200);

        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.start();
    }

    @SuppressLint("NewApi")
    public void showView(final View view) {
        ViewPropertyAnimator animator = view.animate()
                .translationY(0)
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(200);

        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.start();
    }

    /**
     * Setup the streams Drawer which has a {@link ExpandableListView} categorizes the stream and subject
     */
    private void setupListViewAdapter() {
        streamsDrawerAdapter = null;
        String[] groupFrom = {Stream.NAME_FIELD, Stream.COLOR_FIELD, ExpandableStreamDrawerAdapter.UNREAD_TABLE_NAME};
        int[] groupTo = {R.id.name, R.id.stream_dot, R.id.unread_group};
        // Comparison of data elements and View
        String[] childFrom = {Message.SUBJECT_FIELD, ExpandableStreamDrawerAdapter.UNREAD_TABLE_NAME};
        int[] childTo = {R.id.name_child, R.id.unread_child};
        final ExpandableListView streamsDrawer = (ExpandableListView) findViewById(R.id.streams_drawer);
        streamsDrawer.setGroupIndicator(null);
        try {
                streamsDrawerAdapter = new ExpandableStreamDrawerAdapter(this, getSteamCursorGenerator().call(),
                        R.layout.stream_tile_new, groupFrom,
                        groupTo, R.layout.stream_tile_child, childFrom,
                        childTo);
        } catch (Exception e) {
            ZLog.logException(e);
        }

        streamsDrawer.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                switch (v.getId()) {
                    case R.id.name_child_container:
                        String streamName = ((Cursor) streamsDrawer.getExpandableListAdapter().getGroup(groupPosition)).getString(1);
                        String subjectName = ((Cursor) streamsDrawer.getExpandableListAdapter().getChild(groupPosition, childPosition)).getString(0);
                        onNarrow(new NarrowFilterStream(streamName, subjectName));
                        onNarrowFillSendBoxStream(streamName, subjectName, false);
                        break;
                    default:
                        return false;
                }
                return false;
            }
        });
        streamsDrawer.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            int previousClick = -1;

            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int position, long l) {
                resetStreamSearch();
                String streamName = ((TextView) view.findViewById(R.id.name)).getText().toString();
                doNarrow(new NarrowFilterStream(streamName, null));
                drawerLayout.openDrawer(GravityCompat.START);
                if (previousClick != -1 && expandableListView.getCount() > previousClick) {
                    expandableListView.collapseGroup(previousClick);
                }
                expandableListView.expandGroup(position);
                previousClick = position;
                return true;
            }
        });
        streamsDrawerAdapter.setViewBinder(new SimpleCursorTreeAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch (view.getId()) {
                    case R.id.name:
                        TextView name = (TextView) view;
                        final String streamName = cursor.getString(columnIndex);
                        name.setText(streamName);
                        //Change color in the drawer if this stream is inHomeView only.
                        if (!Stream.getByName(app, streamName).getInHomeView()) {
                            name.setTextColor(ContextCompat.getColor(ZulipActivity.this, R.color.colorTextTertiary));
                        } else {
                            name.setTextColor(ContextCompat.getColor(ZulipActivity.this, R.color.colorTextPrimary));
                        }
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetStreamSearch();
                                onNarrow(new NarrowFilterStream(streamName, null));
                                onNarrowFillSendBoxStream(streamName, "", false);
                            }
                        });
                        return true;
                    case R.id.stream_dot:
                        // Set the color of the (currently white) dot
                        view.setVisibility(View.VISIBLE);
                        view.getBackground().setColorFilter(cursor.getInt(columnIndex),
                                PorterDuff.Mode.MULTIPLY);
                        return true;
                    case R.id.unread_group:
                        TextView unreadGroupTextView = (TextView) view;
                        final String unreadGroupCount = cursor.getString(columnIndex);
//                        if (unreadGroupCount.equals("0")) {
//                            unreadGroupTextView.setVisibility(View.GONE);
//                        } else {
//                            unreadGroupTextView.setText(unreadGroupCount);
//                            unreadGroupTextView.setVisibility(View.VISIBLE);
//                        }
                        return true;
                    case R.id.unread_child:
                        TextView unreadChildTextView = (TextView) view;
                        final String unreadChildNumber = cursor.getString(columnIndex);
                        if (unreadChildNumber.equals("0")) {
                            unreadChildTextView.setVisibility(View.GONE);
                        } else {
                            unreadChildTextView.setText(unreadChildNumber);
                            unreadChildTextView.setVisibility(View.VISIBLE);
                        }
                        return true;
                    case R.id.name_child:
                        TextView name_child = (TextView) view;
                        name_child.setText(cursor.getString(columnIndex));
                        if (mMutedTopics.isTopicMute(cursor.getInt(1), cursor.getString(columnIndex))) {
                            name_child.setTextColor(ContextCompat.getColor(ZulipActivity.this, R.color.colorTextSecondary));
                        }
                        return true;
                }
                return false;
            }
        });
        streamsDrawer.setAdapter(streamsDrawerAdapter);
    }

    /**
     * Change streamsDrawerAdapter cursor to default
     * Clear text of etSearchStream
     * Remove focus of etSearchStream
     */
    private void resetStreamSearch() {
        try {
            streamsDrawerAdapter.changeCursor(getSteamCursorGenerator().call());
        } catch (Exception e) {
            ZLog.logException(e);
        }
        etSearchStream.setText("");
        //hide soft keyboard
        hideSoftKeyBoard();
        //remove focus
        etSearchStream.clearFocus();
    }

    /**
     * Initiates the streams Drawer if the streams in the drawer is 0.
     */
    public void checkAndSetupStreamsDrawer() {
        setupListViewAdapter();
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
                    ZLog.logException(e);
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
        final String sendingMsg = getResources().getString(R.string.sending_message);
        sendingMessage(true, sendingMsg);
        MessageType messageType = isCurrentModeStream() ? MessageType.STREAM_MESSAGE : MessageType.PRIVATE_MESSAGE;
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
                sendingMessage(false, sendingMsg);
            }

            public void onTaskFailure(String result) {
                Log.d("onTaskFailure", "Result: " + result);
                Toast.makeText(ZulipActivity.this, R.string.message_error, Toast.LENGTH_SHORT).show();
                sendingMessage(false, sendingMsg);
            }
        });
        sender.execute();
    }

    /**
     * Disable chatBox and show a loading footer while sending the message.
     */
    private void sendingMessage(boolean isSending, String message) {
        streamActv.setEnabled(!isSending);
        textView.setEnabled(!isSending);
        messageEt.setEnabled(!isSending);
        topicActv.setEnabled(!isSending);
        sendBtn.setEnabled(!isSending);
        togglePrivateStreamBtn.setEnabled(!isSending);
        if (isSending) {
            TextView msg = (TextView) composeStatus.findViewById(R.id.sending_message);
            msg.setText(message);
            composeStatus.setVisibility(View.VISIBLE);
        }
        else
            composeStatus.setVisibility(View.GONE);
    }

    private LinearLayout composeStatus;

    /**
     * Setup adapter's for the {@link AutoCompleteTextView}
     *
     * These adapters are being intialized -
     *
     * {@link #streamActvAdapter} Adapter for suggesting all the stream names in this AutoCompleteTextView
     * {@link #emailActvAdapter} Adapter for suggesting all the person email's in this AutoCompleteTextView
     * {@link #subjectActvAdapter} Adapter for suggesting all the topic for the stream specified in the {@link #streamActv} in this AutoCompleteTextView
     */
    private void setUpAdapter() {
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
                    ZLog.logException(e);
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
                    ZLog.logException(e);
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
                        int lastIndex = text.lastIndexOf(',');
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
                    ZLog.logException(e);
                    Log.e("SQLException", "SQL not correct", e);
                    return null;
                }
            }
        });

        sendingMessage(false, getResources().getString(R.string.sending_message));
    }

    /**
     * Creates a cursor to get the streams saved in the database
     * @param streamName Filter out streams name containing this string
     */
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

    /**
     * Creates a cursor to get the topics in the stream in
     * @param stream from which topics similar to {@param subject} are selected
     * @param  subject Filter out subject containing this string
     */
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

    /**
     * Creates a cursor to get the E-Mails stored in the database
     * @param email Filter out emails containing this string
     */
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

    private void switchToStream() {
        removeEditTextErrors();
        if (!isCurrentModeStream()) {
            switchView();
        }
    }

    private void switchToPrivate() {
        removeEditTextErrors();
        if (isCurrentModeStream()) {
            switchView();
        }
    }

    private boolean isCurrentModeStream() {
        //The TextView is VISIBLE which means currently send to stream is on.
        return textView.getVisibility() == View.VISIBLE;
    }

    private void removeEditTextErrors() {
        streamActv.setError(null);
        topicActv.setError(null);
        messageEt.setError(null);
    }

    /**
     * Switch from Private to Stream or vice versa in chatBox
     */
    private void switchView() {
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

    private String tempStreamSave = null;

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
        if (narrowedList == null) {

            if (backPressedOnce) {
                finish();
            }

            backPressedOnce = true;
            Toast.makeText(this, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();
            statusUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    backPressedOnce = false;
                }
            };

            statusUpdateHandler.postDelayed(statusUpdateRunnable, 2000);


        }   else {
              narrowedList = null;
              getSupportFragmentManager().popBackStack(NARROW,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
                    ZLog.logException(e);
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

    private void narrow_pm_with(final Person person) {
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
        if (android.os.Build.VERSION.SDK_INT >= 11 && getSupportActionBar() != null) {
            if (title != null) getSupportActionBar().setTitle(title);
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    /**
     * This method creates a new Instance of the MessageListFragment and displays it with the filter.
     */
    public void doNarrow(NarrowFilter filter) {
        narrowedList = MessageListFragment.newInstance(filter);
        // Push to the back stack if we are not already narrowed
        pushListFragment(narrowedList, NARROW);
        narrowedList.onReadyToDisplay(true);
        showView(appBarLayout);
    }

    @Override
    public void onNarrowFillSendBoxPrivate(Person peopleList[], boolean openSoftKeyboard) {
        displayChatBox(true);
        displayFAB(false);
        switchToPrivate();
        ArrayList<String> names = new ArrayList<String>();
        if (peopleList.length == 1) {
            names.add(peopleList[0].getEmail());
        } else {
            for (Person person : peopleList) {
                if (person.getId() != app.getYou().getId()) {
                    names.add(person.getEmail());
                }
            }
        }
        topicActv.setText(TextUtils.join(", ", names));
        messageEt.requestFocus();
        if (openSoftKeyboard) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    /**
     * Fills the chatBox according to the {@link MessageType}
     * @param openSoftKeyboard If true open's up the SoftKeyboard else not.
     */
    @Override
    public void onNarrowFillSendBox(Message message, boolean openSoftKeyboard) {
        displayChatBox(true);
        displayFAB(false);
        if (message.getType() == MessageType.PRIVATE_MESSAGE) {
            switchToPrivate();
            topicActv.setText(message.getReplyTo(app));
            messageEt.requestFocus();
        } else {
            switchToStream();
            streamActv.setText(message.getStream().getName());
            topicActv.setText(message.getSubject());
            if ("".equals(message.getSubject())) {
                topicActv.requestFocus();
            } else messageEt.requestFocus();
        }
        if (openSoftKeyboard) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    /**
     * Fills the chatBox with the stream name and the topic
     * @param stream Stream name to be filled
     * @param subject Subject to be filled
     * @param openSoftKeyboard If true open's the softKeyboard else not
     */
    public void onNarrowFillSendBoxStream(String stream, String subject, boolean openSoftKeyboard) {
        displayChatBox(true);
        displayFAB(false);
        switchToStream();
        streamActv.setText(stream);
        topicActv.setText(subject);
        if ("".equals(subject)) {
            topicActv.requestFocus();
        } else messageEt.requestFocus();
        if (openSoftKeyboard) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.d("ASD", "onCreateOptionsMenu: ");
        if (this.logged_in) {
            getMenuInflater().inflate(R.menu.options, menu);
            prepareSearchView(menu);
            return true;
        }

        return false;
    }

    private boolean prepareSearchView(Menu menu) {
        if (this.logged_in && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Get the SearchView and set the searchable configuration
            final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            // Assumes current activity is the searchable activity
            final MenuItem mSearchMenuItem = menu.findItem(R.id.search);
            final android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(getApplicationContext(), ZulipActivity.class)));
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            ((EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text)).setHintTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
            searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    doNarrow(new NarrowFilterSearch(s));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        mSearchMenuItem.collapseActionView();
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
            case R.id.daynight:
                switch (AppCompatDelegate.getDefaultNightMode()) {
                    case -1:
                    case AppCompatDelegate.MODE_NIGHT_NO:
                        setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    case AppCompatDelegate.MODE_NIGHT_YES:
                        setNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    default:
                        setNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                }
                break;
            case R.id.refresh:
                Log.w("menu", "Refreshed manually by user. We shouldn't need this.");
                onRefresh();
                break;
            case R.id.today:
                doNarrow(new NarrowFilterToday());
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
     * Switches the current Day/Night mode to Night/Day mode
     * @param nightMode which Mode {@link android.support.v7.app.AppCompatDelegate.NightMode}
     */
    private void setNightMode(@AppCompatDelegate.NightMode int nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode);

        if (Build.VERSION.SDK_INT >= 11) {
            recreate();
        }
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
    private void openLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    private void openLegal() {
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

        // Set up the BroadcastReceiver to trap GCM messages so notifications
        // don't show while in the app
        IntentFilter filter = new IntentFilter(GcmBroadcastReceiver.getGCMReceiverAction(getApplicationContext()));
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
            statusUpdateHandler.removeCallbacks(statusUpdateRunnable);
        }
    }

    /**
     * Refresh the current user profile, removes all the tables from the database and reloads them from the server, reset the queue.
     */
    private void onRefresh() {
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

    private void startRequests() {
        Log.i("zulip", "Starting requests");

        if (event_poll != null) {
            event_poll.abort();
            event_poll = null;
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
