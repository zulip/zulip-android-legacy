package com.humbughq.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HumbugActivity extends Activity {
    public static final String SERVER_URI = "https://app.humbughq.com/";
    public static final String USER_AGENT = "HumbugMobile 1.0";

    LinearLayout tilepanel;

    ArrayList<Message> messages;
    HashMap<String, Bitmap> profile_pictures;

    AsyncPoller current_poll;

    boolean suspended = false;

    boolean logged_in = false;

    String api_key;

    String email;

    HumbugActivity that = this; // self-ref

    protected LinearLayout renderStreamMessage(Message message) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);

        LinearLayout envelopeTile = new LinearLayout(this);
        envelopeTile.setOrientation(LinearLayout.HORIZONTAL);

        tile.addView(envelopeTile);

        TextView stream2 = new TextView(this);
        stream2.setText(message.getDisplayRecipient());
        stream2.setTypeface(Typeface.DEFAULT_BOLD);
        stream2.setGravity(Gravity.CENTER_HORIZONTAL);
        stream2.setPadding(10, 10, 10, 5);

        TextView instance = new TextView(this);
        instance.setText(message.getSubject());
        instance.setPadding(10, 0, 10, 10);

        envelopeTile.addView(stream2);
        envelopeTile.addView(instance);

        TextView senderName = new TextView(this);

        senderName.setText(message.getSender());
        senderName.setPadding(10, 10, 10, 10);

        tile.addView(senderName);

        TextView contentView = new TextView(this);
        String content = message.getContent().replaceAll("\\<.*?>", "");
        contentView.setText(content);
        contentView.setPadding(10, 0, 10, 10);

        tile.addView(contentView);

        return tile;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("funny", "starting...");

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
        return;

    }

    protected void openLogin() {
        messages = new ArrayList<Message>();
        this.profile_pictures = new HashMap<String, Bitmap>();

        setContentView(R.layout.main);
        tilepanel = (LinearLayout) findViewById(R.id.tilepanel);

        this.current_poll = new AsyncPoller(this);
        this.current_poll.execute(-1, -1);

    }

    protected void onPause() {
        super.onPause();
        Log.i("status", "suspend");
        this.suspended = true;
    }

    protected void onResume() {
        super.onResume();
        Log.i("status", "resume");
        this.suspended = false;
        if (this.logged_in) {
            this.current_poll = new AsyncPoller(this);
            this.current_poll.execute(HumbugActivity.SERVER_URI);
        }
    }
}
