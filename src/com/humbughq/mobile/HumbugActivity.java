package com.humbughq.mobile;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.Callable;

import android.annotation.TargetApi;
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
import android.support.v4.app.FragmentTransaction;
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
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;

public class HumbugActivity extends FragmentActivity implements
        MessageListFragment.Listener {

    ZulipApp app;

    // Intent Extra constants
    public enum Flag {
        RESET_DATABASE,
    }

    public enum LoadPosition {
        ABOVE, BELOW, NEW, INITIAL,
    }

    boolean suspended = false;
    boolean logged_in = false;

    HumbugActivity that = this; // self-ref
    SharedPreferences settings;
    String client_id;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private Menu menu;
    public Person you;

    protected HashMap<String, Bitmap> gravatars = new HashMap<String, Bitmap>();

    private AsyncGetEvents event_poll;

    MessageListFragment currentList;
    MessageListFragment narrowedList;
    MessageListFragment homeList;

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

    protected RefreshableCursorAdapter streamsAdapter;
    protected RefreshableCursorAdapter peopleAdapter;

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

        homeList = MessageListFragment.newInstance(null);
        pushListFragment(homeList, false);
    }

    private void pushListFragment(MessageListFragment list, boolean back) {
        currentList = list;
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.list_fragment_container, list);
        if (back) {
            transaction.addToBackStack(null);
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
                boolean result = app.resetDatabase();
                Log.i("params", "Database deleted successfully.");
                this.finish();
                break;
            }
        }
    }

    protected void narrow(final Stream stream) {
        doNarrow(new NarrowFilterStream(stream));
    }

    protected void narrow_pm_with(final Person person) {
        doNarrow(new NarrowFilterPM(person));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onListResume(MessageListFragment list) {
        currentList = list;

        NarrowFilter filter = list.filter;

        if (filter == null) {
            getActionBar().setTitle("Zulip");
            getActionBar().setSubtitle(null);
            this.drawerToggle.setDrawerIndicatorEnabled(true);
        } else {
            String title = list.filter.getTitle();
            if (title != null) {
                getActionBar().setTitle(title);
            }
            getActionBar().setSubtitle(list.filter.getSubtitle());
            this.drawerToggle.setDrawerIndicatorEnabled(false);
        }

        this.drawerLayout.closeDrawers();
    }

    protected void doNarrow(NarrowFilter filter) {
        narrowedList = MessageListFragment.newInstance(filter);
        // Push to the back stack if we are not already narrowed
        pushListFragment(narrowedList, currentList == homeList);
        narrowedList.onReadyToDisplay();
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
            getSupportFragmentManager().popBackStack();
            break;
        case R.id.compose_stream:
            String stream = null;
            if (currentList.filter != null) {
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

        event_poll = new AsyncGetEvents(this);
        event_poll.start();
    }

    public void onReadyToDisplay() {
        homeList.onReadyToDisplay();
    }

    public void onMessages(Message[] messages, LoadPosition pos) {
        homeList.onMessages(messages, pos);
        if (narrowedList != null) {
            narrowedList.onMessages(messages, pos);
        }
    }
}
