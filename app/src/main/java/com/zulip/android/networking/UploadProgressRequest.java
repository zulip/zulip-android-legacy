package com.zulip.android.networking;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * TODO: add description
 */

public class UploadProgressRequest extends RequestBody {
    private File mFile;
    private UploadCallbacks mListener;
    private int mNotificationId;
    int counter = 0;

    private static final int DEFAULT_BUFFER_SIZE = 2048;

    public interface UploadCallbacks {
        void onProgressUpdate(int percentage, String progress, int notificationId);
    }

    public UploadProgressRequest(final File file, final  UploadCallbacks listener, int notificationId) {
        mFile = file;
        mListener = listener;
        mNotificationId = notificationId;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("multipart/form-data");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = mFile.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        FileInputStream in = new FileInputStream(mFile);
        long uploaded = 0;

        // workaround logging interceptor disturbing progress update
        counter++;

        try {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {
                uploaded += read;
                sink.write(buffer, 0, read);

                // update progress on UI thread only when httpLoggingInterceptor
                // is not calling writeTo()
                if (counter % 2 == 0) {
                    handler.post(new ProgressUpdater(uploaded, fileLength));
                }
            }
        } finally {
            in.close();
        }
    }

    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;

        public ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            double currentProgress = Math.round((mUploaded / Math.pow(1024, 2)) * 100) / 100.0;
            double totalProgress = Math.round((mTotal / Math.pow(1024, 2)) * 100) / 100.0;
            String progress = currentProgress + " MB"
                    + " / " + totalProgress + " MB";
            mListener.onProgressUpdate((int)(100 * mUploaded / mTotal), progress, mNotificationId);
        }
    }
}