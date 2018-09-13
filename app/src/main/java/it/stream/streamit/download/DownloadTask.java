package it.stream.streamit.download;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import it.stream.streamit.R;

public class DownloadTask extends AsyncTask<String, Integer, String> {
    private Context context;
    private String fileName;
    private String file_url;
    private SQLiteDatabase database;
    private String title, img, artist, year;

    private static final String CANCEL_DOWNLOAD = "it.stream.streamit.download.CANCEL_DOWNLOAD";
    private int notification_id;

    DownloadTask(Context context, String fileName, SQLiteDatabase database, String title, String img, String artist, String year) {
        this.context = context;
        this.fileName = fileName;
        this.database = database;
        this.title = title;
        this.img = img;
        this.artist = artist;
        this.year = year;
        registerCancelDownload();

        notification_id = generateNotificationId();
    }

    @Override
    protected String doInBackground(String... strings) {

        file_url = strings[0];

        InputStream input = null;
        FileOutputStream output = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(strings[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server Return HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
            }

            int fileLength = connection.getContentLength();

            input = connection.getInputStream();

            output = context.getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE);

            byte data[] = new byte[1024];
            long total = 0;
            int count;

            while ((count = input.read(data)) != 1) {
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                if (fileLength > 0) {
                    publishProgress((int) (total * 100 / fileLength));
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
            context.unregisterReceiver(cancelDownload);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.download_notification);
        remoteViews.setTextViewText(R.id.fileName, fileName);
        remoteViews.setProgressBar(R.id.progress, 100, values[0], false);

        Intent intent = new Intent(CANCEL_DOWNLOAD);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getResources().getString(R.string.notification_channel_id));
        builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_file_download)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContent(remoteViews)
                .setCustomContentView(remoteViews)
                .setOngoing(true)
                .addAction(R.drawable.ic_clear, "Cancel", pendingIntent);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(notification_id, builder.build());

    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        if (s != null) {
            Toast.makeText(context, "Download Error: " + s, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "File Downloaded", Toast.LENGTH_SHORT).show();
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.cancel(notification_id);
            try {
                database.execSQL("INSERT INTO downloads (file, file_url, title, img, artist, year) VALUES ('" + fileName + "','" + file_url + "','" + title + "','" + img + "','" + artist + "','" + year + "')");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
            cancelIt();
        }
    };

    private void registerCancelDownload() {
        IntentFilter filter = new IntentFilter(CANCEL_DOWNLOAD);
        context.registerReceiver(cancelDownload, filter);
    }

    private void cancelIt() {
        this.cancel(true);
    }
}
