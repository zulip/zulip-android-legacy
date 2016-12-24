package com.zulip.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.widget.ImageView;

/**
 * This class contains helpers functions for photo uploads.
 */
public class PhotoHelper {

    /**
     * This function is used to decode a scaled Image. Based on {@param rotate}
     * it rotates the bitmap before setting it as source of {@param ImageView}
     *
     * @param imageView on which the bitmap formed is set
     * @param photoPath file path of captured image
     */
    public static void setPic(ImageView imageView, String photoPath, boolean rotate) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);

        if (rotate) {
            // rotate bitmap by 90 degrees
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = null;
            if (bitmap != null) {
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
            }

            imageView.setImageBitmap(rotatedBitmap);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }
}
