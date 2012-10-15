package com.humbughq.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HumbugActivity extends Activity {
    LinearLayout tilepanel;
    
    ArrayList<Message> messages;

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
        

        AssetManager am = getAssets();
        ImageView gravatar = new ImageView(this);
 
        BufferedInputStream buf;
        try {
            // TODO don't use a static file
            buf = new BufferedInputStream(am.open("sample.png"));
            gravatar.setImageBitmap(BitmapFactory.decodeStream(buf));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        TextView contentView = new TextView(this);
        String content = message.getContent().replaceAll("\\<.*?>","");
        contentView.setText(content);
        contentView.setPadding(10, 0, 10, 10);
        leftTile.addView(gravatar);
        rightTile.addView(contentView);
        
        return tile;
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        messages = new ArrayList<Message>();
        
        setContentView(R.layout.main);
        tilepanel = (LinearLayout)findViewById(R.id.tilepanel);

        AssetManager am = getAssets();

        try {
            JSONArray objects = new JSONArray((new Scanner(am.open("all_messages_json"))).nextLine());
            for (int i = 0; i < objects.length(); i++) {
                Log.i("json-iter", ""+i);
                Message message = new Message(objects.getJSONObject(i));
                messages.add(message);
                
                if (message.getType() == Message.STREAM_MESSAGE) {
                    tilepanel.addView(this.renderStreamMessage(message));
                }
            }
        } catch (JSONException e) {
            Log.e("json", "parsing error");
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}