package com.zulip.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;

/** Simplified HTTP request API */
public class HTTPRequest {
    ZulipApp app;
    HashMap<String, String> postDataParams;
    HttpURLConnection conn;
    volatile boolean aborting = false;

    HTTPRequest(ZulipApp app) {
        postDataParams = new HashMap<>();
        this.app = app;
    }

    public void setProperty(String key, String value) {
        this.postDataParams.put(key, value);
    }

    public void clearProperties() {
        this.postDataParams.clear();
    }

    public void abort() {
        aborting = true;
        if (conn != null) {
            (new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    conn.disconnect();
                    return null;
                }
            }).execute();
        }
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            if(entry.getValue() != null) {
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        }

        return result.toString();
    }

    String execute(String method, String path) throws IOException {
        String responseString = "<empty>";
        try {
            URL url = new URL(app.getServerURI() + path);
            this.setProperty("client", "Android");
            String authstr = this.app.getEmail() + ":" + this.app.getApiKey();

            if (!method.equals("POST") && !method.equals("DELETE") &&
                    !method.equals("PUT") && !method.equals("GET")){
                throw new IOException("Wrong HTTP method.");
            }

            if (method.equals("GET")) {
                url = new URL(app.getServerURI() + path + "?" + getPostDataString(this.postDataParams));
            }

            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", "Basic "
                    + Base64.encodeToString(authstr.getBytes(), Base64.NO_WRAP));
            conn.setRequestProperty("User-Agent", app.getUserAgent());
            conn.setUseCaches(false);

            if (!method.equals("GET")) {
                conn.setDoInput(true);
                conn.setDoOutput(true);
                DataOutputStream request = new DataOutputStream(conn.getOutputStream());
                String parameters = getPostDataString(this.postDataParams);
                Log.i("HTTP.parameters", parameters);
                request.writeBytes(parameters);
                request.flush();
                request.close();
            }

            Log.i("HTTP.request", conn.getRequestMethod() + " " + conn.getURL());

            // timeout after 60 seconds in ms
            conn.setConnectTimeout(60 * 1000);
            conn.connect();

            InputStream responseStream;

            if(conn.getResponseCode() >= HttpStatus.SC_BAD_REQUEST)
                responseStream = conn.getErrorStream();
            else
                responseStream = conn.getInputStream();

            responseStream = new BufferedInputStream(responseStream);
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

            String line;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }

            responseStreamReader.close();
            responseString = stringBuilder.toString();
            responseStream.close();

            if (conn.getResponseCode() != HttpStatus.SC_OK) {
                Log.e("HTTP", conn.getResponseMessage());
                Log.e("HTTP", responseString);
                throw new HttpResponseException(conn.getResponseCode(), responseString);
            }
        } finally {
            conn.disconnect();
        }

        Log.i("HTTP.response", responseString);
        return responseString;
    }
}
