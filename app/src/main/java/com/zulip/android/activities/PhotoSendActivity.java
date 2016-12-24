package com.zulip.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageView;

import com.zulip.android.R;
import com.zulip.android.util.PhotoHelper;

public class PhotoSendActivity extends AppCompatActivity {

    private String mPhotoPath;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_send);

        // run activity in full screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // get the file path sent from ZulipActivity
        final Intent intent = getIntent();
        mPhotoPath = intent.getStringExtra(Intent.EXTRA_TEXT);

        mImageView = (ImageView) findViewById(R.id.photoImageView);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        PhotoHelper.setPic(mImageView, mPhotoPath, true);
    }
}
