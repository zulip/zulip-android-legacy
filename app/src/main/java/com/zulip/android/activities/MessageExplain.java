package com.zulip.android.activities;

/**
 * Created by Minarva on 11-10-16.
 */
import android.app.ExpandableListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zulip.android.R;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class MessageExplain extends AppCompatActivity {
    ShowcaseView showcaseView;
    TextView body;
    int flag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_explain);
        final Button startButton = (Button)findViewById(R.id.start_button);
        final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(44.0f);

        showcaseView = new ShowcaseView.Builder(this)
                .withNewStyleShowcase()
                .setTarget(new ViewTarget(R.id.more_options, this))
                .setContentTitle("Long press on the Message to get more options")
                .setStyle(R.style.CustomShowcaseTheme2)
                .build();
        flag = 0;
        body = (TextView)findViewById(R.id.more_options);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent i = new Intent(MessageExplain.this, ShowFeature.class);
                    startActivity(i);

            }
        });
    }
}
