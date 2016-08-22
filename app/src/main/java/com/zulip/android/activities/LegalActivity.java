package com.zulip.android.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.zulip.android.R;

/**
 * Activity to display Legal Information
 */
public class LegalActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        WebView webView = (WebView) findViewById(R.id.legalWebView);
        webView.loadUrl("file:///android_asset/legal.html");
    }

    public void onCloseButton(View v) {
        finish();
    }
}
