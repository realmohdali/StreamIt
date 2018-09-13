package it.stream.streamit.download;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

import it.stream.streamit.R;

public class DownloadService extends Service {

    private int progress, notification_id;
    private String channel_name;
    private SQLiteDatabase database;
    private String fileName;
    private String file_url;

    private String title, img, artist, year;

    private Context context = this;

    private DownloadTask downloadTask;

    private int file_length;

    private static final String CANCEL_DOWNLOAD = "it.stream.streamit.download.CANCEL_DOWNLOAD";
    public static final String DOWNLOADING = "it.stream.streamit.download.DOWNLOADING";


    private RemoteViews remoteViews;
    private NotificationCompat.Builder builder;

    @Override
    public void onCreate() {
        super.onCreate();
        registerCancelDownload();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        channel_name = getResources().getString(R.string.notification_channel_name);
        database = openOrCreateDatabase("favorite", MODE_PRIVATE, null);

        notification_id = generateNotificationId();

        file_url = intent.getExtras().getString("file_url");
        fileName = intent.getExtras().getString("file_name");
        title = intent.getExtras().getString("title");
        img = intent.getExtras().getString("image");
        artist = intent.getExtras().getString("artist");
        year = intent.getExtras().getString("year");

        downloadTask = new DownloadTask();
        downloadTask.execute();

        return super.onStartCommand(intent, flags, startId);
    }

    private void showNotification() {
        remoteViews = new RemoteViews(getPackageName(), R.layout.download_notification);
        remoteViews.setTextViewText(R.id.fileName, fileName);
        remoteViews.setProgressBar(R.id.progress, 100, progress, false);

        Intent intent = new Intent(CANCEL_DOWNLOAD);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                0, intent, 0);


        builder = new NotificationCompat.Builder(this, channel_name);
        builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_file_download)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContent(remoteViews)
                .setCustomContentView(remoteViews)
                .setOngoing(true)
                .addAction(R.drawable.ic_clear, "Cancel", pendingIntent);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(notification_id, builder.build());
    }


    private void updateNotification() {
        remoteViews.setProgressBar(R.id.progress, 100, progress, false);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(notification_id, builder.build());
    }

    private int generateNotificationId() {
        int max = 110;
        int min = 101;
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    private BroadcastReceiver cancelDownload = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
            notificationManagerCompat.cancel(notification_id);
            downloadTask.cancel(true);
            try {
                database.execSQL("DELETE FROM downloads WHERE file = '" + fileName + "'");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            stopSelf();
        }
    };

    private void registerCancelDownload() {
        IntentFilter filter = new IntentFilter(CANCEL_DOWNLOAD);
        registerReceiver(cancelDownload, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cancelDownload);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.cancel(notification_id);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class DownloadTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;

            try {
                showNotification();
                URL url = new URL(file_url);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server Return HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                }

                file_length = connection.getContentLength();

                input = connection.getInputStream();

                output = getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE);

                byte data[] = new byte[1024];
                long total = 0;
                int count;

                int i = 0;

                while ((count = input.read(data)) > 0) {
                    if (isCancelled()) {
                        input.close();
                        return "canceled";
                    }
                    total += count;
                    if (file_length > 0) {
                        i++;
                        if (i == 50) {
                            publishProgress((int) (total * 100 / file_length));
                            i = 0;
                        }
                    }
                    output.write(data, 0, count);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progress = values[0];
            updateNotification();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Toast.makeText(context, "Download Error: " + s, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DOWNLOADING);
                sendBroadcast(intent);
                stopSelf();
            } else {
                Toast.makeText(context, "File Downloaded", Toast.LENGTH_SHORT).show();
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                notificationManagerCompat.cancel(notification_id);
                try {
                    database.execSQL("INSERT INTO downloads (file, file_url, title, img, artist, year, size) VALUES ('" + fileName + "','" + file_url + "','" + title + "','" + img + "','" + artist + "','" + year + "','" + file_length + "')");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(DOWNLOADING);
                sendBroadcast(intent);
                stopSelf();
            }
        }
    }
}