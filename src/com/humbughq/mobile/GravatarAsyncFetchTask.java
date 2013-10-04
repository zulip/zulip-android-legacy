package com.humbughq.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;

class GravatarAsyncFetchTask extends AsyncTask<URL, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private URL url;
    private Person person;
    private HumbugActivity context;

    public GravatarAsyncFetchTask(HumbugActivity context, ImageView imageView,
            Person person) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<ImageView>(imageView);
        this.person = person;
        this.context = context;
    }

    // Fetch and decode image in background.
    @Override
    protected Bitmap doInBackground(URL... urls) {
        url = urls[0];
        Log.i("GAFT.fetch", "Getting gravatar from url: " + url);
        URLConnection connection;
        try {
            connection = url.openConnection();
            connection.setUseCaches(true);
            Object response = connection.getContent();
            if (response instanceof InputStream) {
                return BitmapFactory.decodeStream((InputStream) response);
            }
        } catch (IOException e) {
            ZLog.logException(e);
        }
        return null;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }

        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            final GravatarAsyncFetchTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask && imageView != null) {
                imageView.setImageBitmap(bitmap);
                context.gravatars.put(person.getEmail(), bitmap);
            }
        }
    }

    /*
     * This object is attached to the ImageView at the time of a gravatar
     * request to keep a reference back to the task that is fetching the image.
     * This is to prevent multiple tasks being initiated for the same ImageView.
     */
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<GravatarAsyncFetchTask> taskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                GravatarAsyncFetchTask bitmapWorkerTask) {
            super(res, bitmap);
            taskReference = new WeakReference<GravatarAsyncFetchTask>(
                    bitmapWorkerTask);
        }

        public GravatarAsyncFetchTask getBitmapWorkerTask() {
            return taskReference.get();
        }
    }

    // Called externally to initiate a new gravatar fetch task, only if there
    // already isn't one
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void loadBitmap(HumbugActivity context, URL url,
            ImageView imageView, Person person) {
        if (cancelPotentialWork(person, imageView)) {
            Log.i("GAFT.init", "Starting new task for " + imageView);
            final GravatarAsyncFetchTask task = new GravatarAsyncFetchTask(
                    context, imageView, person);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(
                    context.getResources(), BitmapFactory.decodeResource(
                            context.getResources(),
                            android.R.drawable.presence_online), task);
            imageView.setImageDrawable(asyncDrawable);
            try {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
            } catch (NoSuchFieldError e) {
                task.execute(url);
            }
        }
    }

    public static boolean cancelPotentialWork(Person person, ImageView imageView) {
        final GravatarAsyncFetchTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Person bitmapData = bitmapWorkerTask.person;
            if (!bitmapData.equals(person)) {
                // Cancel previous task
                Log.i("GAFT.refetch", imageView
                        + " was recycled, fetching new gravatar");
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was
        // cancelled
        return true;
    }

    private static GravatarAsyncFetchTask getBitmapWorkerTask(
            ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
}