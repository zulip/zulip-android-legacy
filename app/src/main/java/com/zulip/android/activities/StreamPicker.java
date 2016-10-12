package com.zulip.android.activities;

/**
 * Created by Minarva on 11-10-16.
 */
import android.app.ExpandableListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zulip.android.R;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class StreamPicker extends AppCompatActivity {
    ShowcaseView showcaseView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stream_picker);
        showcaseView = new ShowcaseView.Builder(this)
                .withNewStyleShowcase()
                .setTarget(new ViewTarget(R.id.results, this))
                .setContentText("Switch to a stream and start a chat")
                .setStyle(R.style.CustomShowcaseTheme2)
                .build();


        final Button startButton = (Button)findViewById(R.id.button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                {

                    Intent i = new Intent(StreamPicker.this, ZulipActivity.class);
                    startActivity(i);
                }

            }
        });
    }
}
