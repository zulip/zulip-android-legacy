package com.humbughq.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("funny", "starting...");

        settings = getPreferences(Activity.MODE_PRIVATE);

        this.email = settings.getString("email", null);
        this.api_key = settings.getString("api_key", null);

        if (this.api_key == null) {

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
        } else {
            this.openLogin();
        }
        return;

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.compose:
            return true;
        case R.id.logout:
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected void openLogin() {
        messageIndex = new SparseArray<Message>();
        this.profile_pictures = new HashMap<String, Bitmap>();

        setContentView(R.layout.main);
        listView = (ListView) findViewById(R.id.listview);

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
            this.current_poll = new AsyncPoller(this, true, true);
            this.current_poll.execute(
                    ((Message) this.listView.getSelectedItem()).getID(),
                    "newer", 1000);
        }
    }
}
