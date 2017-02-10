package com.zulip.android.activities;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.zulip.android.R;
import com.zulip.android.util.ZLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


public class PhotoViewActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {


    private static final int EXTERNAL_STORAGE_PERMISSION_CONSTANT = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;
    int id = 1;
    private ImageView linkImage;
    private ProgressBar progressBar;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean openSettings = false;
    private SharedPreferences permissionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        permissionStatus = getSharedPreferences("permissionStatus", MODE_PRIVATE);
        final Intent intent = getIntent();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        getSupportActionBar().setTitle(url);
        linkImage = (ImageView) findViewById(R.id.linkImageView);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        GlideDrawableImageViewTarget imageViewPreview = new GlideDrawableImageViewTarget(linkImage);
        Glide
                .with(this)
                .load(url)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        if (!isNetworkAvailable()) {
                            Toast.makeText(getApplicationContext(), R.string.toast_no_internet_connection, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.toast_unable_to_load_image, Toast.LENGTH_SHORT).show();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(imageViewPreview);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photoview_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Intent intent = getIntent();
        final String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        switch (item.getItemId()) {
            case R.id.open:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
            case R.id.copy_link:
                copyLink(url);
                break;
            case R.id.download:
                if (ActivityCompat.checkSelfPermission(PhotoViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    allowPermission();
                } else {
                    downloadImage(url);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void allowPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(PhotoViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //Information about the permission
            AlertDialog.Builder builder = new AlertDialog.Builder(PhotoViewActivity.this);
            builder.setTitle(R.string.permission_title);
            builder.setMessage(R.string.permission_message);
            builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    ActivityCompat.requestPermissions(PhotoViewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_CONSTANT);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

        } else if (permissionStatus.getBoolean(Manifest.permission.WRITE_EXTERNAL_STORAGE, false)) {
            //Previously Permission Request was cancelled with 'Dont Ask Again',
            // Now Redirect to Settings after showing Information about permission
            AlertDialog.Builder builder = new AlertDialog.Builder(PhotoViewActivity.this);
            builder.setTitle(R.string.permission_title);
            builder.setMessage(R.string.permission_message);
            builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    openSettings = true;
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

        } else {

            ActivityCompat.requestPermissions(PhotoViewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_CONSTANT);
        }

        SharedPreferences.Editor editor = permissionStatus.edit();
        editor.putBoolean(Manifest.permission.WRITE_EXTERNAL_STORAGE, true);
        editor.apply();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == EXTERNAL_STORAGE_PERMISSION_CONSTANT) {

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                final Intent intent = getIntent();
                final String url = intent.getStringExtra(Intent.EXTRA_TEXT);
                downloadImage(url);

            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void downloadImage(String url) {
        Toast.makeText(PhotoViewActivity.this, R.string.downloading, Toast.LENGTH_SHORT).show();
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(PhotoViewActivity.this);
        mBuilder.setContentTitle(url)
                .setSmallIcon(android.R.drawable.stat_sys_download);
        DownloadImage download = new DownloadImage();
        download.execute(url);

    }

    private void copyLink(String url) {
        ClipboardManager clipboard
                = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("link", url);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(PhotoViewActivity.this, R.string.link_copied, Toast.LENGTH_SHORT).show();
    }

    class DownloadImage extends AsyncTask<String, Integer, String> {

        int calculatedProgress = 0;
        int imageLength = 0;

        @Override
        protected void onPreExecute() {

            mBuilder.setProgress(100, 0, false);
            mNotifyManager.notify(id, mBuilder.build());

        }

        @Override
        protected String doInBackground(String... params) {

            String path = params[0];
            File downloadedImages = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + Uri.parse(path).getLastPathSegment());
            int total = 0;
            int count = 0;

            try {
                URL url = new URL(path);
                URLConnection urlConnection = url.openConnection();
                urlConnection.connect();
                imageLength = urlConnection.getContentLength();
                boolean isFileCreated = downloadedImages.exists();
                InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);
                byte[] data = new byte[1024];
                OutputStream outputStream = new FileOutputStream(downloadedImages);

                if (!isFileCreated) {
                    isFileCreated = downloadedImages.createNewFile();
                }
                while ((count = inputStream.read(data)) != -1) {
                    total = total + count;
                    outputStream.write(data, 0, count);
                    calculatedProgress = (int) total * 100 / imageLength;
                    publishProgress(total);
                }
                inputStream.close();
                outputStream.close();

            } catch (MalformedURLException e) {
                ZLog.logException(e);
            } catch (IOException e) {
                ZLog.logException(e);
            }

            return String.valueOf(downloadedImages);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            mBuilder.setProgress(100, values[0], true);
            mNotifyManager.notify(id, mBuilder.build());
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(String result) {

            mBuilder.setContentText("Download complete");
            mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            mBuilder.setProgress(0, 0, false);
            Toast.makeText(PhotoViewActivity.this, "Saved at " + result, Toast.LENGTH_LONG).show();
            File file = new File(result);
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            PendingIntent pIntent = PendingIntent.getActivity(PhotoViewActivity.this, 0, intent, 0);
            mBuilder.setContentIntent(pIntent).build();
            mBuilder.setAutoCancel(true);
            mNotifyManager.notify(id, mBuilder.build());

        }
    }
}