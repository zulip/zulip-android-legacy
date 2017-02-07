package com.zulip.android.util;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Common progress dialog used in the entire app
 * In the later stage if we change progress style then changes should be done here only
 */

public class CommonProgressDialog {

    private ProgressDialog progressDialog;

    public CommonProgressDialog(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    public void show() {
        progressDialog.show();
    }

    public void showWithMessage(String message){
        setMessage(message);
        show();
    }

    public void dismiss() {
        progressDialog.dismiss();
    }

    public boolean isShowing() {
        return progressDialog.isShowing();
    }

    public void setMessage(String message) {
        progressDialog.setMessage(message);
    }
}
