package com.zulip.android.util;

import com.zulip.android.ZulipApp;

public class UrlHelper {

    public static String addHost(String url) {
        if (!url.startsWith("http")) {
            url = ZulipApp.get().getServerHostUri() + url;
        }

       return url;
    }
}
