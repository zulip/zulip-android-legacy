package com.zulip.android.networking;

import android.util.Base64;

import com.zulip.android.ZulipApp;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by patrykpoborca on 8/25/16.
 */
public class ZulipInterceptor implements Interceptor {

    private final ZulipApp app;

    public ZulipInterceptor() {
        app = ZulipApp.get();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();
        requestBuilder.addHeader("client", "Android");
        requestBuilder.addHeader("User-Agent", app.getUserAgent());

        if (app.getApiKey() != null) {
            String authstr = app.getEmail() + ":" + app.getApiKey();
            requestBuilder.addHeader("Authorization", "Basic " + Base64.encodeToString(authstr.getBytes(), Base64.NO_WRAP));
        }

        Request request = requestBuilder.build();
        ZulipApp.get().badRequest = request;
        return chain.proceed(request);
    }
}
