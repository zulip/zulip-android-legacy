package com.zulip.android.activities;


/**
 * Created by Minarva on 12-10-16.
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

public class ChatBoxFeature extends AppCompatActivity {
    ShowcaseView showcaseView;
    TextView body;
    int flag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_box_feature);
        final Button startButton = (Button)findViewById(R.id.button);
        final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(44.0f);

        showcaseView = new ShowcaseView.Builder(this)
                .withNewStyleShowcase()
                .setTarget(new ViewTarget(R.id.chatbox, this))
                .setContentTitle("Swipe left or right to remove the chat box")
                .setStyle(R.style.CustomShowcaseTheme2)
                .build();
        flag = 0;
        body = (TextView)findViewById(R.id.switch_stream);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flag == 0) {
                    showcaseView = new ShowcaseView.Builder(ChatBoxFeature.this)
                            .withNewStyleShowcase()
                            .setTarget(new ViewTarget(R.id.switch_stream, ChatBoxFeature.this)).setContentTitlePaint(titlePaint)
                            .setContentTitle("Press here to choose a stream")
                            .setStyle(R.style.CustomShowcaseTheme2)
                            .build();
                    showcaseView.show();
                    flag = 1;

                }else{
                    Intent i = new Intent(ChatBoxFeature.this, StreamPicker.class);
                    startActivity(i);
                }

            }
        });
    }
}