package com.humbughq.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HumbugActivity extends Activity {
    public static final String SERVER_URI = "http://10.0.2.2:8000/";

    LinearLayout tilepanel;

    ArrayList<Message> messages;
    HashMap<String, Bitmap> profile_pictures;

    AsyncPoller current_poll;

    boolean suspended = false;

    public LinearLayout renderStreamMessage(Message message) {
        LinearLayout tile = new LinearLayout(this);

        LinearLayout leftTile = new LinearLayout(this);
        leftTile.setOrientation(LinearLayout.VERTICAL);

        LinearLayout rightTile = new LinearLayout(this);
        rightTile.setOrientation(LinearLayout.VERTICAL);

        tile.addView(leftTile);
        tile.addView(rightTile);

        TextView stream2 = new TextView(this);
        stream2.setText(message.getDisplayRecipient());
        stream2.setTypeface(Typeface.DEFAULT_BOLD);
        stream2.setGravity(Gravity.CENTER_HORIZONTAL);
        stream2.setPadding(10, 10, 10, 10);

        TextView instance = new TextView(this);
        instance.setText(message.getSubject());
        instance.setPadding(10, 10, 10, 10);

        leftTile.addView(stream2);
        rightTile.addView(instance);

        TextView senderName = new TextView(this);
        senderName.setWidth(100);
        senderName.setGravity(Gravity.CENTER_HORIZONTAL);
        senderName.setText(message.getSender());

        TextView contentView = new TextView(this);
        String content = message.getContent().replaceAll("\\<.*?>", "");
        contentView.setText(content);
        contentView.setPadding(10, 0, 10, 10);
        leftTile.addView(senderName);
        rightTile.addView(contentView);

        return tile;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("funny", "starting...");
        messages = new ArrayList<Message>();
        this.profile_pictures = new HashMap<String, Bitmap>();

        setContentView(R.layout.main);
        tilepanel = (LinearLayout) findViewById(R.id.tilepanel);

        this.current_poll = new AsyncPoller(this);
        this.current_poll.execute(HumbugActivity.SERVER_URI);

    }

    public void onPause() {
        super.onPause();
        Log.i("status", "suspend");
        this.suspended = true;
    }

    public void onResume() {
        super.onResume();
        Log.i("status", "resume");
        this.suspended = false;
        this.current_poll = new AsyncPoller(this);
        this.current_poll.execute(HumbugActivity.SERVER_URI);
    }
}
