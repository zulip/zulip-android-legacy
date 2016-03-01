package com.zulip.android;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

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
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.j256.ormlite.android.AndroidDatabaseResults;

import io.fabric.sdk.android.Fabric;

public class ZulipActivity extends FragmentActivity implements
        MessageListFragment.Listener {

    ZulipApp app;

    // Intent Extra constants
    public enum Flag {
        RESET_DATABASE,
    }

    boolean suspended = false;
    boolean logged_in = false;

    ZulipActivity that = this; // self-ref
    SharedPreferences settings;
    String client_id;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private Menu menu;

    protected HashMap<String, Bitmap> gravatars = new HashMap<String, Bitmap>();

    private AsyncGetEvents event_poll;

    private Handler statusUpdateHandler;

    MessageListFragment currentList;
    MessageListFragment narrowedList;
    MessageListFragment homeList;

    Notifications notifications;

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
            }
            return false;
        }
    };

    protected RefreshableCursorAdapter streamsAdapter;
    protected RefreshableCursorAdapter peopleAdapter;

    /** Called when the activity is first created. */
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.HARDWARE.contains("goldfish")) {
            Log.i("hardware", "running in emulator");
        } else {
            Fabric.with(this, new Crashlytics());
        }

        app = (ZulipApp) getApplicationContext();
        settings = app.settings;

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

                String[] columnsWithPresence = new String[] { "_id",
                        Person.EMAIL_FIELD, Person.NAME_FIELD };

                MatrixCursor sortedPeopleCursor = new MatrixCursor(
                        columnsWithPresence);
                for (Person person : people) {
                    Object[] row = new Object[] { person.id, person.getEmail(),
                            person.getName() };
                    sortedPeopleCursor.addRow(row);
                }

                // add private messages row
                MatrixCursor allPrivateMessages = new MatrixCursor(
                        sortedPeopleCursor.getColumnNames());
                Object[] row = new Object[] { allPeopleId, "",
                        "All private messages" };

                allPrivateMessages.addRow(row);

                MergeCursor mergeCursor = new MergeCursor(new Cursor[] {
                        allPrivateMessages, sortedPeopleCursor });
                return mergeCursor;

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
                    peopleGenerator.call(), peopleGenerator, new String[] {
                    Person.NAME_FIELD, Person.EMAIL_FIELD }, new int[] {
                    R.id.name, R.id.stream_dot }, 0);
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
                narrow(Stream.getById(app, (int) id));
            }
        });

        peopleDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (id == allPeopleId) {
                    doNarrow(new NarrowFilterAllPMs(app.you));
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
                    public void onTaskComplete(String result) {
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
    }

    public void onBackPressed() {
        if (narrowedList != null) {
            narrowedList = null;
            getSupportFragmentManager().popBackStack("narrow",
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
                    Log.e("params", "Invalid app-specific intent specified.", e);
                    continue;
                }
            } else {
                continue;
            }
            switch (param) {
                case RESET_DATABASE:
                    Log.i("params", "Resetting the database...");
                    app.resetDatabase();
                    Log.i("params", "Database deleted successfully.");
                    this.finish();
                    break;
            }
        }
    }

    protected void narrow(final Stream stream) {
        doNarrow(new NarrowFilterStream(stream, null));
    }

    protected void narrow_pm_with(final Person person) {
        doNarrow(new NarrowFilterPM(Arrays.asList(app.you, person)));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onListResume(MessageListFragment list) {
        currentList = list;

        NarrowFilter filter = list.filter;

        if (filter == null) {
            if (android.os.Build.VERSION.SDK_INT >= 11) {
                getActionBar().setTitle("Zulip");
                getActionBar().setSubtitle(null);
            }
            this.drawerToggle.setDrawerIndicatorEnabled(true);
        } else {
            String title = list.filter.getTitle();
            if (android.os.Build.VERSION.SDK_INT >= 11) {
                if (title != null) {
                    getActionBar().setTitle(title);
                }
                getActionBar().setSubtitle(list.filter.getSubtitle());
            }
            this.drawerToggle.setDrawerIndicatorEnabled(false);
        }

        this.drawerLayout.closeDrawers();
    }

    public void doNarrow(NarrowFilter filter) {
        narrowedList = MessageListFragment.newInstance(filter);
        // Push to the back stack if we are not already narrowed
        pushListFragment(narrowedList, "narrow");
        narrowedList.onReadyToDisplay(true);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            final SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView searchView =(SearchView) menu.findItem(R.id.search).getActionView();


            final MenuItem mSearchMenuItem = menu.findItem(R.id.search);
            final SearchView searchView =
                    (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(
                    new ComponentName(getApplicationContext(),
                            ZulipActivity.class)));


            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));


            searchView.setIconifiedByDefault(false);

            searchView
                    .setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
            drawerLayout.closeDrawer(Gravity.RIGHT);
            return true;
        }

        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack("narrow",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
            case R.id.search:
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

            case R.id.compose_stream:
                String stream = null;
                if (currentList.filter != null
                        && currentList.filter.getComposeStream() != null) {
                    stream = currentList.filter.getComposeStream().getName();
                }
                openCompose(MessageType.STREAM_MESSAGE, stream, null, null);
                break;
            case R.id.compose_pm:
                String recipient = null;
                if (currentList.filter != null) {
                    recipient = currentList.filter.getComposePMRecipient();
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

    public void openCompose(MessageType type) {
        openCompose(type, null, null, null);
    }

    public void openCompose(Stream stream, String topic) {
        openCompose(MessageType.STREAM_MESSAGE, stream.getName(), topic, null);
    }

    public void openCompose(String pmRecipients) {
        openCompose(MessageType.PRIVATE_MESSAGE, null, null, pmRecipients);
    }

    public void openCompose(final MessageType type, String stream,
                            String topic, String pmRecipients) {

        FragmentManager fm = getSupportFragmentManager();
        ComposeDialog dialog = ComposeDialog.newInstance(type, stream, topic,
                pmRecipients);
        dialog.show(fm, "fragment_compose");
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
        app.setEmail(app.you.getEmail());
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

    private BroadcastReceiver onGcmMessage = new BroadcastReceiver() {
        public void onReceive(Context contenxt, Intent intent) {
            // Block the event before it propagates to show a notification.
            // TODO: could be smarter and only block the event if the message is
            // in the narrow.
            Log.i("GCM", "Dropping a push because the activity is active");
            abortBroadcast();
        }
    };
}
