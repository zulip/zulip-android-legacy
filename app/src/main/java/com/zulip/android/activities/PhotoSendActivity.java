package com.zulip.android.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.zulip.android.R;
import com.zulip.android.util.PhotoHelper;

import java.io.File;

public class PhotoSendActivity extends AppCompatActivity {

    private String mPhotoPath;
    private ImageView mImageView;
    private CropImageView mCropImageView;
    private boolean mIsCropFinished;
    private boolean mIsCropped;

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

        // intent to go back to ZulipActivity and upload photo
        final Intent sendIntent = new Intent(this, ZulipActivity.class);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        ImageView deleteBtn = (ImageView) findViewById(R.id.delete_photo);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(mPhotoPath);
                boolean isFileDeleted = file.delete();
                if (!isFileDeleted) {
                    Log.e("Photo upload", "Could not delete photo");
                }

                // go back to ZulipActivity to start camera intent
                startActivity(sendIntent);
            }
        });

        mCropImageView = (CropImageView) findViewById(R.id.crop_image_view);

        final ImageView cropBtn = (ImageView) findViewById(R.id.crop_btn);
        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsCropFinished) {
                    Bitmap bitmap;
                    Drawable drawable = mImageView.getDrawable();
                    if (drawable instanceof GlideBitmapDrawable) {
                        // if imageview has drawable of type GlideBitmapDrawable
                        bitmap = ((GlideBitmapDrawable)mImageView.getDrawable().getCurrent())
                                .getBitmap();

                    } else {
                        // if imageView stores cropped image drawable which is of type drawable
                        bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                    }

                    // if image is to be cropped, make CropImageView visible
                    mCropImageView.setImageBitmap(bitmap);
                    mCropImageView.setVisibility(View.VISIBLE);

                    // tint the crop button blue during cropping
                    cropBtn.setColorFilter(ContextCompat.getColor(PhotoSendActivity.this,
                            R.color.photo_buttons));
                    mIsCropFinished = true;
                    mIsCropped = true;
                } else {
                    // set cropped image as source of ImageView
                    Bitmap croppedImage = mCropImageView.getCroppedImage();
                    mCropImageView.setVisibility(View.GONE);
                    mImageView.setImageBitmap(croppedImage);

                    // tint the crop button white when cropping is finished
                    cropBtn.setColorFilter(Color.WHITE);
                    mIsCropFinished = false;
                }
            }
        });

        ImageView sendPhoto = (ImageView) findViewById(R.id.send_photo);
        sendPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsCropped) {
                    // if image was cropped, delete old file
                    // and store new bitmap on that location
                    Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                    mPhotoPath = PhotoHelper.saveBitmapAsFile(mPhotoPath, bitmap);
                }

                // add the file path of cropped image
                sendIntent.putExtra(Intent.EXTRA_TEXT, mPhotoPath);
                startActivity(sendIntent);
            }
        });

        ImageView editPhotoBtn = (ImageView) findViewById(R.id.edit_photo);
        editPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsCropped) {
                    Bitmap bitmap;
                    Drawable drawable = mImageView.getDrawable();
                    if (drawable instanceof GlideBitmapDrawable) {
                        // if imageview has drawable of type GlideBitmapDrawable
                        bitmap = ((GlideBitmapDrawable)mImageView.getDrawable().getCurrent())
                                .getBitmap();

                    } else {
                        // if imageView stores cropped image drawable which is of type drawable
                        bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                    }

                    // if image was cropped, delete old file
                    // and store new bitmap on that location
                    mPhotoPath = PhotoHelper.saveBitmapAsFile(mPhotoPath, bitmap);
                }

                // start PhotoEditActivity, passing it the file path for cropped photo
                Intent intent = new Intent(PhotoSendActivity.this, PhotoEditActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, mPhotoPath);
                startActivity(intent);
            }
        });

    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // use glide to take care of high performance bitmap decoding
        if (!mIsCropped) {
            Glide.with(this).load(mPhotoPath).crossFade().into(mImageView);
        }
    }
}
