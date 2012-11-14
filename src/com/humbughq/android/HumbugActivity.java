package com.humbughq.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class HumbugActivity extends Activity {
    public static final String SERVER_URI = "https://app.humbughq.com/";
    public static final String USER_AGENT = "HumbugMobile 1.0";

    ListView listView;

    HashMap<String, Bitmap> profile_pictures;

    /*
     * A "message" refers to an instance of the object Message. A "tile" is an
     * instance of LinearLayout which represents a single message in the UI.
     */

    SparseArray<Message> messageIndex;
    MessageAdapter adapter;

    AsyncPoller current_poll;

    boolean suspended = false;

    boolean logged_in = false;

    String api_key;

    String email;

    HumbugActivity that = this; // self-ref
    SharedPreferences settings;
    String client_id;
    protected int mIDSelected;
    private Menu menu;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("funny", "starting...");

        settings = getPreferences(Activity.MODE_PRIVATE);

        this.email = settings.getString("email", null);
        this.api_key = settings.getString("api_key", null);

        if (this.api_key == null) {
            this.openLogin();
        } else {
            this.openHomeView();
        }

        return;

    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu == null) {
            // we're getting called before the menu exists, bail
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
        case R.id.compose:
            return true;
        case R.id.logout:
            logout();
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void logout() {
        this.logged_in = false;

        if (this.current_poll != null) {
            this.current_poll.cancel(true);
        }

        Editor ed = this.settings.edit();

        ed.remove("email");
        ed.remove("api_key");
        ed.commit();

        this.openLogin();

    }

    protected void openLogin() {
        this.logged_in = false;

        this.onPrepareOptionsMenu(menu);

        setContentView(R.layout.login);

        ((Button) findViewById(R.id.login))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TextView errorText = (TextView) findViewById(R.id.error_text);
                        errorText.setText("Logging in...");
                        (new AsyncLogin(that,
                                ((EditText) findViewById(R.id.username))
                                        .getText().toString(),
                                ((EditText) findViewById(R.id.password))
                                        .getText().toString())).execute();
                    }
                });
    }

    protected void openHomeView() {
        this.onPrepareOptionsMenu(menu);

        this.logged_in = true;
        messageIndex = new SparseArray<Message>();
        this.profile_pictures = new HashMap<String, Bitmap>();

        setContentView(R.layout.main);
        listView = (ListView) findViewById(R.id.listview);

        /*
         * We want to add a footer to the ListView that is half the window
         * height.
         * 
         * Adapted from
         * http://stackoverflow.com/questions/13366281/how-can-i-add
         * -blank-space-to-the-end-of-a-listview#13366310
         */
        WindowManager wm = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        @SuppressWarnings("deprecation")
        // needed for compat with API >13
        int screenHeight = display.getHeight();

        View layout = new ImageView(this);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                screenHeight / 2, 0);
        lp.height = screenHeight / 2;
        layout.setLayoutParams(lp);
        listView.addFooterView(layout);

        adapter = new MessageAdapter(this, new ArrayList<Message>());
        listView.setAdapter(adapter);

        listView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
                // pass

            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                int mID = ((Message) view.getItemAtPosition(view
                        .getFirstVisiblePosition())).getID();
                if (mIDSelected != mID) {
                    Log.i("scrolling", "Now at " + mID);
                    (new AsyncPointerUpdate(that)).execute(mID);
                    mIDSelected = mID;
                }
            }
        });

        listView.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                int mID = (Integer) view.getTag(R.id.messageID);
                if (mIDSelected != mID) {
                    Log.i("keyboard", "Now at " + mID);
                    (new AsyncPointerUpdate(that)).execute(mID);
                    mIDSelected = mID;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // pass

            }

        });
        (new AsyncPointerUpdate(this)).execute();

    }

    protected void onPause() {
        super.onPause();
        Log.i("status", "suspend");
        this.suspended = true;
        if (this.current_poll != null) {
            this.current_poll.cancel(true);
        }
    }

    protected void onResume() {
        super.onResume();
        Log.i("status", "resume");
        this.suspended = false;
        if (this.logged_in) {
            // Update the pointer

            if (this.adapter.getCount() != 0) {
                this.current_poll = new AsyncPoller(this, true, true);
                this.current_poll.execute((int) this.adapter
                        .getItemId(this.adapter.getCount() - 1) + 1);
            }
        }
    }
}
