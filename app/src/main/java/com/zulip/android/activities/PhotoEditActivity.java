package com.zulip.android.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.zulip.android.R;
import com.zulip.android.util.DrawCustomView;
import com.zulip.android.util.PhotoHelper;

public class PhotoEditActivity extends AppCompatActivity {

    private String mPhotoPath;
    private ImageView mImageView;
    private DrawCustomView mDrawCustomView;
    private SimpleTarget mGlideTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_edit);

        // run activity in full screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // get file path of image from PhotoSendActivity
        final Intent intent = getIntent();
        mPhotoPath = intent.getStringExtra(Intent.EXTRA_TEXT);

        mImageView = (ImageView) findViewById(R.id.photoImageView);
        mDrawCustomView = (DrawCustomView) findViewById(R.id.draw_custom_view);

        // glide target called when image is loaded
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        mGlideTarget = new SimpleTarget<Bitmap>(width, height) {
            @Override
            public void onResourceReady(Bitmap bitmap, GlideAnimation glideAnimation) {
                // set bitmap on imageView
                mImageView.setImageBitmap(bitmap);

                // bound the canvas for drawing to the actual dimensions of imageView
                int[] imageDimensions = PhotoHelper.getBitmapPositionInsideImageView(mImageView);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        imageDimensions[2],
                        imageDimensions[3]
                );
                params.setMargins(imageDimensions[0], imageDimensions[1], 0, 0);
                mDrawCustomView.setLayoutParams(params);
            }
        };

        // use glide to take care of high performance bitmap decoding
        Glide
                .with(this)
                .load(mPhotoPath)
                .asBitmap()
                .into(mGlideTarget);
    }
}
