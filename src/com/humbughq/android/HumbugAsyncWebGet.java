package com.humbughq.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

class HumbugAsyncWebGet extends AsyncTask<String, String, String> {

    HumbugActivity that;

    public static final String USER_AGENT = "HumbugMobile 1.0";

    public HumbugAsyncWebGet(HumbugActivity humbugActivity) {
        that = humbugActivity;
    }

    protected String doInBackground(String... uri) {
        AndroidHttpClient httpclient = AndroidHttpClient
                .newInstance(HumbugAsyncWebGet.USER_AGENT);
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else {
                // Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            // TODO Handle problems..
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        httpclient.close();

        return responseString;
    }

}