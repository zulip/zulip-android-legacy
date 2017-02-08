package com.zulip.android.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.zulip.android.R;
import com.zulip.android.util.ActivityTransitionAnim;
import com.zulip.android.util.PhotoHelper;

import java.io.File;

public class PhotoSendActivity extends AppCompatActivity {

    private String mPhotoPath;
    private ImageView mImageView;
    private CropImageView mCropImageView;
    private boolean mIsCropFinished;
    private boolean mIsCropped;
    private ImageView mCropBtn;
    private SimpleTarget<Bitmap> mGlideTarget;
    private Intent mIntentReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_send);

        // run activity in full screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // get the file path sent from ZulipActivity
        mIntentReceived = getIntent();
        mPhotoPath = mIntentReceived.getStringExtra(Intent.EXTRA_TEXT);

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

                // activity transition animation
                ActivityTransitionAnim.transition(PhotoSendActivity.this);
            }
        });

        mCropImageView = (CropImageView) findViewById(R.id.crop_image_view);

        mCropBtn = (ImageView) findViewById(R.id.crop_btn);
        mCropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsCropFinished) {
                    Bitmap bitmap;
                    Drawable drawable = mImageView.getDrawable();
                    if (drawable instanceof GlideBitmapDrawable) {
                        // if imageview has drawable of type GlideBitmapDrawable
                        bitmap = ((GlideBitmapDrawable) mImageView.getDrawable().getCurrent())
                                .getBitmap();

                    } else {
                        // if imageView stores cropped image drawable which is of type drawable
                        bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                    }

                    // if image is to be cropped, make CropImageView visible
                    mCropImageView.setImageBitmap(bitmap);
                    mCropImageView.setVisibility(View.VISIBLE);

                    // tint the crop button blue during cropping
                    mCropBtn.setColorFilter(ContextCompat.getColor(PhotoSendActivity.this,
                            R.color.photo_buttons));
                    mIsCropFinished = true;
                    mIsCropped = true;
                } else {
                    // set cropped image as source of ImageView
                    Bitmap croppedImage = mCropImageView.getCroppedImage();
                    mCropImageView.setVisibility(View.GONE);
                    mImageView.setImageBitmap(croppedImage);

                    // tint the crop button white when cropping is finished
                    mCropBtn.setColorFilter(Color.WHITE);
                    mIsCropFinished = false;
                }
            }
        });

        ImageView sendPhoto = (ImageView) findViewById(R.id.send_photo);
        sendPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bitmap;
                Drawable drawable = mImageView.getDrawable();
                if (drawable instanceof GlideBitmapDrawable) {
                    // if imageview has drawable of type GlideBitmapDrawable
                    bitmap = ((GlideBitmapDrawable) mImageView.getDrawable().getCurrent())
                            .getBitmap();

                } else {
                    // if imageView stores cropped image drawable which is of type drawable
                    bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                }

                /* Most phone cameras are landscape, meaning if you take the photo in portrait,
                   the resulting photos will be rotated 90 degrees. Hence used the displayed image
                   with correct orientation and saved that in the current photo path.
                */

                // delete old file and store new bitmap on that location
                // used the displayed image and saved it in the mPhotoPath
                mPhotoPath = PhotoHelper.saveBitmapAsFile(mPhotoPath, bitmap);

                // add the file path of cropped image
                sendIntent.putExtra(Intent.EXTRA_TEXT, mPhotoPath);
                startActivity(sendIntent);

                // activity transition animation
                ActivityTransitionAnim.transition(PhotoSendActivity.this);
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
                        bitmap = ((GlideBitmapDrawable) mImageView.getDrawable().getCurrent())
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

                // activity transition animation
                ActivityTransitionAnim.transition(PhotoSendActivity.this);
            }
        });

        // set up cancel button to take user back to where it came from
        ImageView cancelBtn = (ImageView) findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoSendActivity.super.onBackPressed();
            }
        });


        // glide target called when intent is received from PhotoEditActivity to crop
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

                // check if intent is sent from PhotoEditActivity
                Intent intent = PhotoSendActivity.this.getIntent();
                boolean fromEdit = intent.getBooleanExtra(PhotoEditActivity.class.getSimpleName(), false);
                if (fromEdit) {
                    // trigger crop action
                    mCropBtn.performClick();
                }
            }
        };
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // use glide to take care of high performance bitmap decoding
        if (mIntentReceived != null && mIntentReceived.getBooleanExtra(PhotoEditActivity.class.getSimpleName(), false)
                && hasFocus) {
            // use simple target to know when image is loaded
            Glide.with(this).load(mPhotoPath).asBitmap().into(mGlideTarget);
        } else if (!mIsCropped && hasFocus) {
            // load image specified at mPhotoPath in imageView
            Glide.with(this).load(mPhotoPath).crossFade().into(mImageView);
        }
    }

    @Override
    public void onBackPressed() {
        // on pressing back, if image has been cropped
        // its undone
        if (mIsCropped) {
            Glide.with(this).load(mPhotoPath).crossFade().into(mImageView);
            mIsCropped = false;
        } else {
            // otherwise user goes back to where it came from
            super.onBackPressed();
        }
    }
}
