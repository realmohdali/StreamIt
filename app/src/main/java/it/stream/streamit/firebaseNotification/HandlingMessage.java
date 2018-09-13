package it.stream.streamit.firebaseNotification;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import it.stream.streamit.R;
import it.stream.streamit.YearInArtist;

public class HandlingMessage extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        int Notification_ID = generateNotificationId();

        String imageURL = remoteMessage.getData().get("image");
        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("message");
        String artist = remoteMessage.getData().get("artist");

        Intent openAlbum = new Intent(this, YearInArtist.class);
        openAlbum.putExtra("artist", artist);
        openAlbum.putExtra("image", imageURL);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openAlbum, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, getResources().getString(R.string.notification_channel_name));
        mBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(getBitmapFromURL(imageURL))
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(defaultSoundURI)
                .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(getBitmapFromURL(imageURL))
                        .setBigContentTitle(title)
                        .setSummaryText(message)
                        .bigLargeIcon(null));

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(Notification_ID, mBuilder.build());

        super.onMessageReceived(remoteMessage);
    }

    private Bitmap getBitmapFromURL(String imageURL) {
        try {
            URL url = new URL(imageURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            return null;
        }
    }

    private int generateNotificationId() {
        int max = 99;
        int min = 1;
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }
}
