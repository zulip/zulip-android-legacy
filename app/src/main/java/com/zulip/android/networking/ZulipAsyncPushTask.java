package com.zulip.android.networking;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.zulip.android.ZulipApp;
import com.zulip.android.util.ZLog;

import org.json.JSONObject;

import okhttp3.Response;

/**
 *  General AsyncTask for use in making various web requests to Humbug.
 *
 * This class should be extended by each asynchronous operation you
 * want to run. Most clients will need to override onPostExecute.
 */
public abstract class ZulipAsyncPushTask extends AsyncTask<String, String, String> {

    public ZulipApp app;
    private HTTPRequest request;
    AsyncTaskCompleteListener callback;

    /**
     * Interface implemented by callbacks which are run at the end of a task.
     * <p/>
     * Clients overriding onPostExecute will need to finish with
     * <p/>
     * callback.onTaskComplete(result);
     * <p/>
     * if they want to honor declared callback.
     */
    public interface AsyncTaskCompleteListener {
        void onTaskComplete(String result, JSONObject jsonObject);

        void onTaskFailure(String result);
    }

    /**
     * Declares a new HumbugAsyncPushTask, passing the activity as context.
     *
     * @param app the zulip app god object
     */
    public ZulipAsyncPushTask(ZulipApp app) {
        this.app = app;
        callback = new AsyncTaskCompleteListener() {
            @Override
            public void onTaskComplete(String result, JSONObject jsonObject) {
                // Dummy method which does nothing
            }

            public void onTaskFailure(String result) {
            }
        };
        this.request = new HTTPRequest(app);
    }

    /*
     * Target newer API than the general application because
     * THREAD_POOL_EXECUTOR was not available on older Androids. This is okay
     * because on pre-honeycomb, post-donut Android always used a thread pool.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public AsyncTask<String, String, String> execute(String method, String url) {
        try {
            request.setMethodAndUrl(method, url);
            return this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (NoSuchFieldError e) {
            return super.execute(method, url);
        }
    }

    /**
     * Sets the callback to run when the task is complete.
     *
     * @param listener AsyncTaskCompleteListener to run
     */
    public void setCallback(AsyncTaskCompleteListener listener) {
        callback = listener;
    }

    /**
     * Sets a parameter for the request.
     */
    public void setProperty(String key, String value) {
        this.request.setProperty(key, value);
    }

    protected String doInBackground(String... api_path) {
        try {
            Response response = request.execute();
            String responseString = response.body().string();
            if (response.isSuccessful()) {
                Log.d("OkHTTP200", responseString);
                return responseString;
            } else {
                Log.e("OkHTTPError", "Code:" + response.code());
                Log.e("OkHTTPError", "Message:" + responseString);
                this.cancel(true);
                return responseString;
            }
        } catch (Exception e) {
            ZLog.logException(e);
            this.cancel(true);
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result) {
        callback.onTaskComplete(result, null);
    }

    //Override this method for detecting errors!
    @Override
    protected void onCancelled(String result) {
        callback.onTaskFailure(result);
    }
}