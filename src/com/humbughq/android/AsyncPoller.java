package com.humbughq.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

class AsyncPoller extends AsyncTask<String, String, String> {

    HumbugActivity that;

    public AsyncPoller(HumbugActivity humbugActivity) {
        that = humbugActivity;
    }

    @Override
    protected String doInBackground(String... uri) {
        AndroidHttpClient httpclient = AndroidHttpClient
                .newInstance("HumbugMobile 1.0");
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
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        try {
            JSONArray objects = new JSONArray(result);
            for (int i = 0; i < objects.length(); i++) {
                Log.i("json-iter", "" + i);
                Message message = new Message(objects.getJSONObject(i));
                this.that.messages.add(message);

                if (message.getType() == Message.STREAM_MESSAGE) {
                    this.that.tilepanel.addView(this.that
                            .renderStreamMessage(message));
                }
            }
        } catch (JSONException e) {
            Log.e("json", "parsing error");
            e.printStackTrace();
        }
        new AsyncPoller(this.that).execute(HumbugActivity.SERVER_URI);
    }
}