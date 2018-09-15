package it.stream.streamit.download;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import it.stream.streamit.Downloaded;
import it.stream.streamit.R;

public class DownloadService extends Service {

    private int progress, notification_id;
    private SQLiteDatabase database;
    private String fileName;
    private String file_url;

    private String title, img, artist, year;

    private Context context = this;

    private DownloadTask downloadTask;

    private int file_length;

    private static final String CANCEL_DOWNLOAD = "it.stream.streamit.download.CANCEL_DOWNLOAD";
    public static final String DOWNLOADING = "it.stream.streamit.download.DOWNLOADING";

    private String channel_id;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManagerCompat;
    private PendingIntent downloadComplete;

    private SharedPreferences.Editor editor;


    @Override
    public void onCreate() {
        super.onCreate();
        registerCancelDownload();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sp.edit();
        editor.putBoolean("downloadingService", true);
        editor.apply();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        channel_id = getResources().getString(R.string.notification_channel_id);
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
        Intent intent = new Intent(CANCEL_DOWNLOAD);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                0, intent, 0);
        Intent downloaded = new Intent(this, Downloaded.class);
        downloadComplete = PendingIntent.getActivity(this, 0, downloaded, 0);

        builder = new NotificationCompat.Builder(getApplicationContext(), channel_id);
        builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_file_download)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("Downloading")
                .setContentText(fileName)
                .setOngoing(true)
                .addAction(R.drawable.ic_clear, "Cancel", pendingIntent)
                .setProgress(100, 0, false);

        notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.notify(notification_id, builder.build());
    }


    private void updateNotification() {
        builder.setProgress(100, progress, false);
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
        editor.putBoolean("downloadingService", false);
        editor.apply();

        unregisterReceiver(cancelDownload);
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
                builder.setContentTitle("Downloading Fail")
                        .setContentText(fileName)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setProgress(0, 0, false)
                        .mActions.clear();
                notificationManagerCompat.notify(notification_id, builder.build());
                Intent intent = new Intent(DOWNLOADING);
                sendBroadcast(intent);
                stopSelf();
            } else {
                Toast.makeText(context, "File Downloaded", Toast.LENGTH_SHORT).show();
                builder.setContentTitle("File Downloaded")
                        .setContentText(fileName)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setContentIntent(downloadComplete)
                        .setAutoCancel(true)
                        .mActions.clear();
                notificationManagerCompat.notify(notification_id, builder.build());

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