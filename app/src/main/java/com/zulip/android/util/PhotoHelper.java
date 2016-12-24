package com.zulip.android.util;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class contains helpers functions for photo uploads.
 */
public class PhotoHelper {

    /**
     * Function to delete the file at {@param photoPath} and store {@param bitmap}
     * at {@param photoPath}.
     *
     * @param photoPath file path
     * @param bitmap to be saved as file
     */
    public static void saveBitmapAsFile(String photoPath, Bitmap bitmap) {
        // delete old bitmap
        File file = new File(photoPath);
        file.delete();

        // store new bitmap at mPhotoPath file path
        FileOutputStream out = null;
        try {
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
    }
}
