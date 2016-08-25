package com.zulip.android.networking;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.util.ZLog;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Simplified HTTP request API
 * Uses the {@link OkHttpClient} for requests.
 */
public class HTTPRequest {
    private ZulipApp app;
    volatile boolean aborting = false;
    private HashMap<String, String> properties;
    private OkHttpClient okHttpClient;
    private Response response = null;
    private String method, path;
    private Object synchronization = new Object();

    public void setMethodAndUrl(String method, String URL) {
        this.method = method;
        this.path = URL;
    }

    public HTTPRequest(ZulipApp app) {
        properties = new HashMap<>();
        this.app = app;
        okHttpClient = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();
    }


    public void setProperty(String key, String value) {
        properties.put((key == null) ? "" : key, (value == null) ? "" : value);
    }

    void clearProperties() {
        properties.clear();
    }

    void abort() {
        aborting = true;
        synchronized (synchronization) {
            if (response != null) {
                final Response finalResponse = response;
                response = null;
                (new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            finalResponse.body().close();
                        } catch (IllegalStateException e) {
                            //fail silently
                        }
                        return null;
                    }
                }).execute();
            }
        }
    }

    public Response execute() throws IOException {
        if (method == null)
            throw new IOException(app.getString(R.string.method_null));
        Request.Builder requestBuilder = new Request.Builder();
        String url = app.getServerURI() + path;
        requestBuilder.addHeader("client", "Android");
        requestBuilder.addHeader("User-Agent", app.getUserAgent());

        switch (method) {
            case "GET":
                requestBuilder.url(generateURL(url, properties)).get();
                break;
            case "DELETE":
                FormBody.Builder formDeleteBody = new FormBody.Builder();
                for (Map.Entry<String, String> map : properties.entrySet()) {
                    formDeleteBody.add(map.getKey(), map.getValue());
                }
                requestBuilder.url(url).delete(formDeleteBody.build());
                break;
            case "POST":
                FormBody.Builder formBody = new FormBody.Builder();
                for (Map.Entry<String, String> map : properties.entrySet()) {
                    formBody.add(map.getKey(), map.getValue());
                }
                requestBuilder.url(url).post(formBody.build());
                break;
            case "PUT":
                FormBody.Builder builder = new FormBody.Builder();
                for (Map.Entry<String, String> map : properties.entrySet()) {
                    builder.add(map.getKey(), map.getValue());
                }
                requestBuilder.url(url).put(builder.build());
                break;
            default:
                throw new IOException(app.getString(R.string.method_error));
        }

        if (this.app.getApiKey() != null) {
            String authstr = this.app.getEmail() + ":" + this.app.getApiKey();
            requestBuilder.addHeader("Authorization", "Basic " + Base64.encodeToString(authstr.getBytes(), Base64.NO_WRAP));
        }
        Request request = requestBuilder.build();
        Log.i("OkHTTP.request", method + " " + request.url().toString());
        if(request.url().toString().contains("pointer"))
        response = okHttpClient.newCall(request).execute();
        return response;
    }

    private String generateURL(String url, HashMap<String, String> properties) {
        if (properties.isEmpty()) return url;
        String encodedUrl = url + "?";
        for (Map.Entry<String, String> map : properties.entrySet()) {
            try {
                encodedUrl += URLEncoder.encode(map.getKey(), ("utf-8")) + "=" + URLEncoder.encode(map.getValue(), "utf-8") + "&";
            } catch (UnsupportedEncodingException e) {
                ZLog.logException(e);
            }
        }
        StringUtils.removeEnd(encodedUrl, "&");
        return encodedUrl;
    }
}
