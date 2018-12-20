package com.zulip.android.util;

import com.zulip.android.ZulipApp;

public class UrlHelper {

    public static String addHost(String url) {
        if (!url.startsWith("http")) {
            String hostUrl = ZulipApp.get().getServerHostUri();
            if (hostUrl.endsWith("/")) {
                url = hostUrl.substring(0, hostUrl.length() - 1) + url;
            } else {
                url = hostUrl + url;
            }
        }

        return url;
    }
}
