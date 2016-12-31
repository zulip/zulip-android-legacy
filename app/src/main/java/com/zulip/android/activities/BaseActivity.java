package com.zulip.android.activities;

import android.support.v7.app.AppCompatActivity;

import com.zulip.android.ZulipApp;
import com.zulip.android.service.ZulipServices;



public abstract class BaseActivity extends AppCompatActivity {

    protected ZulipApp getApp() {
        return ZulipApp.get();
    }

    protected ZulipServices getServices() {
        return getApp().getZulipServices();
    }
}
