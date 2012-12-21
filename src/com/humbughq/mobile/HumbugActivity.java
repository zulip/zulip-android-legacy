package com.humbughq.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.humbughq.mobile.HumbugAsyncPushTask.AsyncTaskCompleteListener;

public class HumbugActivity extends Activity {
    public static final String USER_AGENT = "HumbugMobile 1.0";

    ListView listView;

    SparseArray<Message> messageIndex;
    MessageAdapter adapter;

    AsyncPoller current_poll;

    boolean suspended = false;

    boolean logged_in = false;

    String api_key;

    HumbugActivity that = this; // self-ref
    SharedPreferences settings;
    String client_id;
    protected int mIDSelected;
    private Menu menu;
    public Person you;
    private Dialog composeWindow;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getPreferences(Activity.MODE_PRIVATE);

        this.you = new Person(null, settings.getString("email", null));
        this.api_key = settings.getString("api_key", null);

        if (this.api_key == null) {
            this.openLogin();
        } else {
            this.openHomeView();
        }

        return;

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
        EditText recipient = (EditText) composeWindow
                .findViewById(R.id.composeRecipient);
        EditText subject = (EditText) composeWindow
                .findViewById(R.id.composeSubject);

        subject.setVisibility(View.GONE);
        recipient.setGravity(Gravity.FILL_HORIZONTAL);
        recipient.setHint(R.string.pm_prompt);
    }

    /**
     * Switches the compose window's state to compose a stream message.
     */
    protected void switchToStream() {
        EditText recipient = (EditText) composeWindow
                .findViewById(R.id.composeRecipient);
        EditText subject = (EditText) composeWindow
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
        this.composeWindow = new Dialog(this);
        composeWindow.setContentView(R.layout.compose);
        composeWindow.setTitle("Compose");

        EditText recipient = (EditText) composeWindow
                .findViewById(R.id.composeRecipient);

        EditText subject = (EditText) composeWindow
                .findViewById(R.id.composeSubject);

        EditText body = (EditText) composeWindow.findViewById(R.id.composeText);

        if (type == MessageType.STREAM_MESSAGE
                || (msg != null && msg.getType() == MessageType.STREAM_MESSAGE)) {
            this.switchToStream();
        } else {
            this.switchToPersonal();
        }
        if (msg != null) {
            if (msg.getType() == MessageType.STREAM_MESSAGE) {
                recipient.setText(msg.getStream());
                subject.setText(msg.getSubject());

            } else {
                recipient.setText(msg.getReplyTo());
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

        Button send = (Button) composeWindow.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText recipient = (EditText) composeWindow
                        .findViewById(R.id.composeRecipient);

                EditText subject = (EditText) composeWindow
                        .findViewById(R.id.composeSubject);

                EditText body = (EditText) composeWindow
                        .findViewById(R.id.composeText);

                // If the subject field is hidden, we have a personal message.
                boolean subjectFilledIfRequired = subject.getVisibility() == View.GONE
                        || requireFilled(subject, "subject");

                if (!(requireFilled(recipient, "recipient")
                        && subjectFilledIfRequired && requireFilled(body,
                        "message body"))) {
                    return;

                }

                Message msg = new Message();
                msg.setSender(that.you);
                if (subject.getVisibility() == View.GONE) {
                    msg.setType(MessageType.PRIVATE_MESSAGE);
                    msg.setRecipient(recipient.getText().toString().split(","));
                } else {
                    msg.setType(MessageType.STREAM_MESSAGE);
                    msg.setStream(recipient.getText().toString());
                    msg.setSubject(subject.getText().toString());
                }

                msg.setContent(body.getText().toString());

                AsyncSend sender = new AsyncSend(that, msg);
                sender.execute();

                composeWindow.dismiss();
            }
        });
        Button cancel = (Button) composeWindow.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                composeWindow.dismiss();
            }
        });

        composeWindow.show();
        composeWindow
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // Hide physical keyboard if present.
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

                    }
                });

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

        if (this.current_poll != null) {
            this.current_poll.cancel(true);
        }

        Editor ed = this.settings.edit();

        ed.remove("email");
        ed.remove("api_key");
        ed.commit();

        this.openLogin();

    }

    /**
     * Switch to the login view.
     */
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
        ((TextView) findViewById(R.id.legalTextView))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openLegal();
                    }
                });
    }

    protected void openLegal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        try {
            InputStream legalStream = this.getResources().getAssets()
                    .open("legal.html", AssetManager.ACCESS_BUFFER);

            WebView legalView = new WebView(this);
            legalView.loadData(new Scanner(legalStream).useDelimiter("\\Z")
                    .next(), "text/html", null);

            builder.setView(legalView)
                    .setTitle(R.string.legal)
                    .setPositiveButton(R.string.close,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.dismiss();
                                }
                            });

            AlertDialog dialog = builder.create();
            dialog.show();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Open the home view, where the message list is displayed.
     */
    protected void openHomeView() {
        this.onPrepareOptionsMenu(menu);

        this.logged_in = true;
        messageIndex = new SparseArray<Message>();

        setContentView(R.layout.main);
        listView = (ListView) findViewById(R.id.listview);

        /*
         * We want to add a footer to the ListView that is half the window
         * height.
         * 
         * Example algorithmic explanation:
         * http://stackoverflow.com/a/13366310/90777
         */

        @SuppressWarnings("deprecation")
        // needed for compat with API <13
        int windowHeight = ((WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getHeight();

        ImageView dummy = new ImageView(this);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(0, 0);
        params.height = windowHeight / 2;
        dummy.setLayoutParams(params);

        listView.addFooterView(dummy);
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
                if (that.logged_in) {
                    // Scrolling messages isn't meaningful unless we have
                    // messages to scroll.
                    int mID = ((Message) view.getItemAtPosition(view
                            .getFirstVisiblePosition())).getID();
                    if (mIDSelected != mID) {
                        Log.i("scrolling", "Now at " + mID);
                        (new AsyncPointerUpdate(that)).execute(mID);
                        mIDSelected = mID;
                    }
                }
            }
        });

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                try {
                    openCompose(adapter.getItem(position));
                } catch (IndexOutOfBoundsException e) {
                    // We can ignore this because its probably before the data
                    // has been fetched.
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
            if (this.adapter.getCount() != 0) {
                this.current_poll = new AsyncPoller(this, true);
                this.current_poll.execute((int) this.adapter
                        .getItemId(this.adapter.getCount() - 1) + 1);

                // Update the pointer on resume.
                this.current_poll.setCallback(new AsyncTaskCompleteListener() {

                    @Override
                    public void onTaskComplete(String result) {
                        (new AsyncPointerUpdate(that)).execute();
                    }

                });
            }
        }
    }

    /**
     * Determines the server URI applicable for the user.
     * 
     * @return either the production or staging server's URI
     */
    public String getServerURI() {
        if (you.getRealm().equals("humbughq.com")) {
            return "https://staging.humbughq.com/";
        }
        return "https://humbughq.com/";
    }
}
