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

    HumbugActivity context;
    List<NameValuePair> nameValuePairs;
    AsyncTaskCompleteListener callback;

    interface AsyncTaskCompleteListener {
        public void onTaskComplete(String result);
    }

    public HumbugAsyncPushTask(HumbugActivity humbugActivity) {
        context = humbugActivity;
        callback = new AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result) {
                // Dummy method which does nothing

            }
        };
        nameValuePairs = new ArrayList<NameValuePair>();
    }

    public void setCallback(AsyncTaskCompleteListener listener) {
        callback = listener;
    }

    public void setProperty(String key, String value) {
        this.nameValuePairs.add(new BasicNameValuePair(key, value));
    }

    protected String doInBackground(String... api_path) {
        AndroidHttpClient httpclient = AndroidHttpClient
                .newInstance(HumbugActivity.USER_AGENT);
        HttpResponse response;
        String responseString = null;
        try {
            HttpPost httppost = new HttpPost(HumbugActivity.SERVER_URI
                    + api_path[0]);
            Log.i("welp", HumbugActivity.SERVER_URI + api_path[0]);

            nameValuePairs.add(new BasicNameValuePair("api-key",
                    this.context.api_key));
            nameValuePairs.add(new BasicNameValuePair("email", this.context.you
                    .getEmail()));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // timeout after 55 seconds in ms
            HttpConnectionParams.setSoTimeout(httppost.getParams(), 55 * 1000);

            response = httpclient.execute(httppost);

            StatusLine statusLine = response.getStatusLine();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            responseString = out.toString();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
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