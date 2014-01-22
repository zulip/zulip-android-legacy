package com.zulip.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

/** Simplified HTTP request API */
public class HTTPRequest {
    ZulipApp app;
    List<NameValuePair> nameValuePairs;
    HttpRequestBase request;
    HttpResponse response;
    volatile boolean aborting = false;

    HTTPRequest(ZulipApp app) {
        nameValuePairs = new ArrayList<NameValuePair>();
        this.app = app;
    }

    public void setProperty(String key, String value) {
        this.nameValuePairs.add(new BasicNameValuePair(key, value));
    }

    public void clearProperties() {
        this.nameValuePairs.clear();
    }

    public void abort() {
        aborting = true;
        if (request != null) {
            (new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    request.abort();
                    return null;
                }
            }).execute();
        }
    }

    // Java doesn't support a request body on DELETE requests, but RFC2616 does
    // not prohibit it.
    class HttpDeleteWithReq extends HttpPost {
        HttpDeleteWithReq(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }

    String execute(String method, String path) throws IOException {
        AndroidHttpClient httpclient = AndroidHttpClient.newInstance(app
                .getUserAgent());

        String responseString = null;
        try {
            String url = app.getServerURI() + path;
            nameValuePairs.add(new BasicNameValuePair("client", "Android"));

            if (method.equals("POST")) {
                request = new HttpPost(url);
            } else if (method.equals("PUT")) {
                request = new HttpPut(url);
            } else if (method.equals("DELETE")) {
                request = new HttpDeleteWithReq(url);
            } else if (method.equals("GET")) {
                request = new HttpGet(url + "?"
                        + URLEncodedUtils.format(nameValuePairs, "utf-8"));
            }

            if (!method.equals("GET")) {
                ((HttpEntityEnclosingRequestBase) request)
                        .setEntity(new UrlEncodedFormEntity(nameValuePairs,
                                "UTF-8"));
            }

            Log.i("HTTP.request", request.getMethod() + " " + request.getURI());

            String authstr = this.app.getEmail() + ":" + this.app.getApiKey();
            request.setHeader(
                    "Authorization",
                    "Basic "
                            + Base64.encodeToString(authstr.getBytes(),
                                    Base64.NO_WRAP));
            // timeout after 60 seconds in ms
            HttpConnectionParams.setSoTimeout(request.getParams(), 60 * 1000);

            response = httpclient.execute(request);

            StatusLine statusLine = response.getStatusLine();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            responseString = out.toString();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                Log.e("HTTP", statusLine.getReasonPhrase());
                Log.e("HTTP", responseString);
                throw new HttpResponseException(statusLine.getStatusCode(),
                        responseString);
            }
        } finally {
            httpclient.close();
        }

        if (responseString != null) {
            Log.i("HTTP.response", responseString);
        } else {
            Log.i("HTTP.response", "<empty>");
        }
        return responseString;
    }
}
