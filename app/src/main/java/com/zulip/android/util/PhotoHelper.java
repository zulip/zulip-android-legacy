package com.zulip.android.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class contains helpers functions for photo uploads used by
 * {@link com.zulip.android.activities.PhotoEditActivity} and
 * {@link com.zulip.android.activities.PhotoSendActivity}
 */
public class PhotoHelper {

    /**
     * Function to delete the file at {@param photoPath} and store {@param bitmap}
     * at {@param photoPath}.
     *
     * @param photoPath file path
     * @param bitmap to be saved as file
     */
    public static String saveBitmapAsFile(String photoPath, Bitmap bitmap) {
        // delete old bitmap
        File file = new File(photoPath);
        file.delete();

        // store new bitmap at mPhotoPath file path
        FileOutputStream out = null;
        try {
            // change file name to avoid catching issues with Glide
            photoPath += Math.round(Math.random() * 10);
            out = new FileOutputStream(photoPath);

            // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            ZLog.logException(e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                ZLog.logException(e);
            }
        }

        return photoPath;
    }

    /**
     * From http://stackoverflow.com/a/26930938/5334314
     * Returns the actual bitmap position in an imageView.
     *
     * @param imageView source ImageView
     * @return 0: left, 1: top, 2: width, 3: height
     */
    public static int[] getBitmapPositionInsideImageView(ImageView imageView) {
        int[] ret = new int[4];

        if (imageView == null || imageView.getDrawable() == null) {
            return ret;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        ret[2] = actW;
        ret[3] = actH;

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - actH) / 2;
        int left = (int) (imgViewW - actW) / 2;

        ret[0] = left;
        ret[1] = top;

        return ret;
    }
}
