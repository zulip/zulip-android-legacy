package com.humbughq.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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
    public static final String SERVER_URI = "http://10.0.2.2:8000/";

    LinearLayout tilepanel;

    ArrayList<Message> messages;
    HashMap<String, Bitmap> profile_pictures;

    AsyncPoller current_poll;

    boolean suspended = false;

    boolean logged_in = false;

    String api_key;

    protected LinearLayout renderStreamMessage(Message message) {
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

        setContentView(R.layout.login);
        ((Button) findViewById(R.id.login))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TextView errorText = (TextView) findViewById(R.id.error_text);
                        errorText.setText("Logging in...");
                        if (!doLogin(((EditText) findViewById(R.id.username))
                                .getText().toString(),
                                ((EditText) findViewById(R.id.password))
                                        .getText().toString())) {
                            // Login failed

                            errorText.setText("Login failed");
                        } else {
                            openLogin();
                        }
                    }
                });
        return;

    }

    protected boolean doLogin(String username, String password) {

        logged_in = false;

        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpPost httppost = new HttpPost(SERVER_URI
                    + "api/v1/fetch_api_key");

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("username", username));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = httpclient.execute(httppost);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                this.logged_in = true;
            }

            Log.i("login", response.getStatusLine().getStatusCode() + "");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            api_key = out.toString();
            Log.i("login", "Logged in as " + api_key);

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
        return logged_in;

    }

    protected void openLogin() {
        messages = new ArrayList<Message>();
        this.profile_pictures = new HashMap<String, Bitmap>();

        setContentView(R.layout.main);
        tilepanel = (LinearLayout) findViewById(R.id.tilepanel);

        this.current_poll = new AsyncPoller(this);
        this.current_poll.execute(HumbugActivity.SERVER_URI);

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
