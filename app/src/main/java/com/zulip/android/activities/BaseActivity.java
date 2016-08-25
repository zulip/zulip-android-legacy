package com.zulip.android.activities;

import android.support.v7.app.AppCompatActivity;

import com.zulip.android.ZulipApp;
import com.zulip.android.service.ZulipServices;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public class BaseActivity extends AppCompatActivity {

    protected ZulipApp getApp() {
        return ZulipApp.get();
    }

    protected ZulipServices getServices() {
        return getApp().getZulipServices();
    }
}
