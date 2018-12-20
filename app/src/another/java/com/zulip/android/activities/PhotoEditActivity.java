package com.zulip.android.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.zulip.android.R;
import com.zulip.android.util.ActivityTransitionAnim;
import com.zulip.android.util.DrawCustomView;
import com.zulip.android.util.PhotoHelper;

import static android.graphics.Bitmap.createBitmap;

public class PhotoEditActivity extends AppCompatActivity {

    private String mPhotoPath;
    private ImageView mImageView;
    private DrawCustomView mDrawCustomView;
    private SimpleTarget mGlideTarget;
    private int[] mImageDimensions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_edit);

        // set a border and color for black marker color
        ImageView black_marker = (ImageView) findViewById(R.id.black_marker);
        GradientDrawable blackCircle = (GradientDrawable) black_marker.getDrawable();
        blackCircle.setColor(ContextCompat.getColor(this, R.color.black_marker_tool));
        blackCircle.setStroke(3, Color.GRAY);

        // change background of marker tool to default color red on activity start up
        int colorId = R.color.red_marker_tool;
        ImageView markerIcon = (ImageView) findViewById(R.id.marker_btn);
        GradientDrawable markerBackground = (GradientDrawable) markerIcon.getBackground();
        markerBackground.setColor(ContextCompat.getColor(this, colorId));
        markerBackground.setStroke(0, Color.GRAY);

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
                mImageDimensions = PhotoHelper.getBitmapPositionInsideImageView(mImageView);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        mImageDimensions[2],
                        mImageDimensions[3]
                );
                params.setMargins(mImageDimensions[0], mImageDimensions[1], 0, 0);
                mDrawCustomView.setLayoutParams(params);
            }
        };

        // use glide to take care of high performance bitmap decoding
        Glide
                .with(this)
                .load(mPhotoPath)
                .asBitmap()
                .into(mGlideTarget);

        // set up undo button
        ImageView undoBtn = (ImageView) findViewById(R.id.undo_btn);
        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawCustomView.onClickUndo();
            }
        });

        // go back when back button is pressed
        ImageView backBtn = (ImageView) findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoEditActivity.super.onBackPressed();
            }
        });

        // set up crop button
        // intent to go back to PhotoSendActivity
        final Intent cropIntent = new Intent(this, PhotoSendActivity.class);
        cropIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TextView cropBtn = (TextView) findViewById(R.id.crop_btn);
        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pass edited photo file path to PhotoSendActivity
                FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_layout_picture);
                frameLayout.setVisibility(View.INVISIBLE);

                // take screenshot of cropped image
                if (frameLayout.getWidth() > 0 && frameLayout.getHeight() > 0) {
                    Bitmap bitmap = screenShot(frameLayout);
                    mPhotoPath = PhotoHelper.saveBitmapAsFile(mPhotoPath, bitmap);

                    cropIntent.putExtra(PhotoEditActivity.class.getSimpleName(), true);
                    cropIntent.putExtra(Intent.EXTRA_TEXT, mPhotoPath);
                    startActivity(cropIntent);

                    // activity transition animation
                    ActivityTransitionAnim.transition(PhotoEditActivity.this);
                } else {
                    // do nothing
                    // wait for layout to be constructed
                }
            }
        });

        // intent to go back to ZulipActivity and upload photo
        // when send button is clicked
        ImageView sendPhoto = (ImageView) findViewById(R.id.send_photo);
        final Intent sendIntent = new Intent(this, ZulipActivity.class);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sendPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // pass edited photo file path to ZulipActivity
                FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_layout_picture);
                frameLayout.setVisibility(View.INVISIBLE);

                // take screenshot of cropped image
                if (frameLayout.getWidth() > 0 && frameLayout.getHeight() > 0) {
                    Bitmap bitmap = screenShot(frameLayout);
                    mPhotoPath = PhotoHelper.saveBitmapAsFile(mPhotoPath, bitmap);

                    sendIntent.putExtra(Intent.EXTRA_TEXT, mPhotoPath);
                    startActivity(sendIntent);

                    // activity transition animation
                    ActivityTransitionAnim.transition(PhotoEditActivity.this);
                } else {
                    // do nothing
                    // wait for layout to be constructed
                }
            }
        });
    }

    /**
     * This function is called when any of the marker colors are chosen.
     * Its sets the color for marker tool and changes its the background.
     *
     * @param view color ImageView
     */
    public void handleMarkerColorChange(View view) {
        int colorId = R.color.red_marker_tool;
        switch (view.getId()) {
            case R.id.red_marker:
                colorId = R.color.red_marker_tool;
                break;
            case R.id.yellow_marker:
                colorId = R.color.yellow_marker_tool;
                break;
            case R.id.green_marker:
                colorId = R.color.green_marker_tool;
                break;
            case R.id.white_marker:
                colorId = R.color.white_marker_tool;
                break;
            case R.id.blue_marker:
                colorId = R.color.blue_marker_tool;
                break;
            case R.id.black_marker:
                colorId = R.color.black_marker_tool;
                break;
            default:
                Log.e("Marker Tool", "Invalid color");
                break;
        }

        // change marker tool color
        mDrawCustomView.setBrushColor(ContextCompat.getColor(this, colorId));

        // change background of marker tool
        ImageView markerIcon = (ImageView) findViewById(R.id.marker_btn);
        GradientDrawable markerBackground = (GradientDrawable) markerIcon.getBackground();
        markerBackground.setColor(ContextCompat.getColor(this, colorId));
        // if black color is selected, add a border to the background of marker tool
        if (colorId == R.color.black_marker_tool) {
            markerBackground.setStroke(3, Color.GRAY);
        } else {
            markerBackground.setStroke(0, Color.GRAY);
        }
    }

    /**
     * Function that takes a screenshot of the view passed and returns a bitmap for it.
     *
     * @param view {@link View}
     * @return screenshot of the view passed
     */
    public Bitmap screenShot(View view) {
        // Define a bitmap with the same size as the view
        Bitmap returnedBitmap = createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.RGB_565);

        // Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);

        // Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            // has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            // does not have background drawable, then draw black background on
            // the canvas
            canvas.drawColor(Color.BLACK);

        // draw the view on the canvas
        view.draw(canvas);

        // obtained only the visible region of edited image
        Bitmap trimmedBitmap = null;
        if (mImageDimensions != null) {
            trimmedBitmap = Bitmap.createBitmap(returnedBitmap,
                    mImageDimensions[0], mImageDimensions[1],
                    mImageDimensions[2], mImageDimensions[3]);
        }
        return trimmedBitmap;
    }
}