package com.zulip.android.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class ZulipWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ZulipRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
