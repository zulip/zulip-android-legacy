package com.humbughq.mobile;

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
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;

import android.annotation.TargetApi;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

/* General AsyncTask for use in making various web requests to Humbug.
 * 
 * This class should be extended by each asynchronous operation you
 * want to run. Most clients will need to override onPostExecute. 
 */
class HumbugAsyncPushTask extends AsyncTask<String, String, String> {

    HumbugActivity context;
    List<NameValuePair> nameValuePairs;
    AsyncTaskCompleteListener callback;

    /**
     * Interface implemented by callbacks which are run at the end of a task.
     * 
     * Clients overriding onPostExecute will need to finish with
     * 
     * callback.onTaskComplete(result);
     * 
     * if they want to honor declared callback.
     */
    interface AsyncTaskCompleteListener {
        public void onTaskComplete(String result);
    }

    /**
     * Declares a new HumbugAsyncPushTask, passing the activity as context.
     * 
     * @param humbugActivity
     *            The Activity that created the PushTask
     */
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

    /*
     * Target newer API than the general application because
     * THREAD_POOL_EXECUTOR was not available on older Androids. This is okay
     * because on pre-honeycomb, post-donut Android always used a thread pool.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public AsyncTask<String, String, String> execute(String method, String url) {
        try {
            return this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, method, url);
        } catch (NoSuchFieldError e) {
            return super.execute(method, url);
        }
    }

    /**
     * Sets the callback to run when the task is complete.
     * 
     * @param listener
     *            AsyncTaskCompleteListener to run
     */
    public void setCallback(AsyncTaskCompleteListener listener) {
        callback = listener;
    }

    /**
     * Sets a POST parameter for the request.
     */
    public void setProperty(String key, String value) {
        this.nameValuePairs.add(new BasicNameValuePair(key, value));
    }

    protected String doInBackground(String... api_path) {
        AndroidHttpClient httpclient = AndroidHttpClient
                .newInstance(HumbugActivity.USER_AGENT);
        HttpResponse response;
        String responseString = null;
        try {
            HttpRequestBase request;
            String method = api_path[0];
            String url = context.getServerURI() + api_path[1];
            nameValuePairs.add(new BasicNameValuePair("client", "Android"));

            // Only POST, PUT and GET are supported, for now.
            if (method.equals("POST")) {
                request = new HttpPost(url);
                ((HttpEntityEnclosingRequestBase) request)
                        .setEntity(new UrlEncodedFormEntity(nameValuePairs,
                                "UTF-8"));
            } else if (method.equals("PUT")) {
                request = new HttpPut(url);
                ((HttpEntityEnclosingRequestBase) request)
                        .setEntity(new UrlEncodedFormEntity(nameValuePairs,
                                "UTF-8"));
            } else {
                request = new HttpGet(url + "?"
                        + URLEncodedUtils.format(nameValuePairs, "utf-8"));
            }

            Log.i("HAPT.request", request.getMethod() + " " + request.getURI());

            String authstr = this.context.you.getEmail() + ":"
                    + this.context.api_key;
            request.setHeader(
                    "Authorization",
                    "Basic "
                            + Base64.encodeToString(authstr.getBytes(),
                                    Base64.NO_WRAP));
            // timeout after 55 seconds in ms
            HttpConnectionParams.setSoTimeout(request.getParams(), 55 * 1000);

            response = httpclient.execute(request);

            StatusLine statusLine = response.getStatusLine();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            responseString = out.toString();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                handleHTTPError(statusLine, responseString);
            }
        } catch (ClientProtocolException e) {
            handleError(e);
        } catch (IOException e) {
            handleError(e);
        }

        httpclient.close();
        if (responseString != null) {
            Log.i("HAPT.response", responseString);
        } else {
            Log.i("HAPT.response", "<empty>");
        }
        return responseString;
    }

    /**
     * 
     * Prints the error reason and response and cancels the task.
     * 
     * This function is called if the server does not return a 200 OK as a
     * response to a request.
     * 
     * Override this to specify custom behavior in your task.
     * 
     * @param statusLine
     *            The StatusLine produced by the request response
     * @param responseString
     *            Data as returned by the server
     */
    protected void handleHTTPError(StatusLine statusLine, String responseString) {
        Log.e("HAPT", statusLine.getReasonPhrase());
        Log.e("HAPT", responseString);
        this.cancel(true);
    }

    /**
     * Prints a stacktrace and cancels the task.
     * 
     * This function is called whenever the backend ends up in an error
     * condition.
     * 
     * Override this to specify custom error behavior in your Task.
     * 
     * @param e
     *            the Exception that triggered this handler
     */
    protected void handleError(Exception e) {
        e.printStackTrace();
        this.cancel(true);
    }
}
