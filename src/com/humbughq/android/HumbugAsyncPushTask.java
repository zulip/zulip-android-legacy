package com.humbughq.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

class HumbugAsyncPushTask extends AsyncTask<String, String, String> {

    HumbugActivity that;

    public static final String USER_AGENT = "HumbugMobile 1.0";

    public HumbugAsyncPushTask(HumbugActivity humbugActivity) {
        that = humbugActivity;
    }

    protected String doInBackground(String... api_path) {
        AndroidHttpClient httpclient = AndroidHttpClient
                .newInstance(HumbugAsyncPushTask.USER_AGENT);
        HttpResponse response;
        String responseString = null;
        try {
            HttpPost httppost = new HttpPost(HumbugActivity.SERVER_URI
                    + api_path[0]);
            Log.i("welp", HumbugActivity.SERVER_URI + api_path[0]);
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("api-key",
                    this.that.api_key));
            nameValuePairs
                    .add(new BasicNameValuePair("email", this.that.email));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpConnectionParams.setSoTimeout(httppost.getParams(),
                    10 * 60 * 1000); // 10 minutes in ms

            response = httpclient.execute(httppost);

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
        Log.i("HAPT", "F" + responseString);
        return responseString;
    }
}