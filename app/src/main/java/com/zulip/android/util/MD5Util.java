package com.zulip.android.util;

import android.util.Log;

import java.io.*;
import java.security.*;

/**
 * Gravatar example from http://en.gravatar.com/site/implement/images/java/
 */
public class MD5Util {

    private static final String TAG = "MD5Util";

    private MD5Util() {
    }

    private static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(
                    1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
