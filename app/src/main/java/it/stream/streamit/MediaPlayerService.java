package it.stream.streamit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static it.stream.streamit.RecentHomeAdapter.Broadcast_PLAY_NEW_AUDIO;

public class MediaPlayerService extends Service
        implements
        android.media.MediaPlayer.OnCompletionListener,
        android.media.MediaPlayer.OnPreparedListener,
        android.media.MediaPlayer.OnErrorListener,
        android.media.MediaPlayer.OnSeekCompleteListener,
        android.media.MediaPlayer.OnInfoListener,
        android.media.MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    //Binder Return to the client
    private final IBinder iBinder = new LocalBinder();

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    //Playlist
    private List<ListItem> mPlaylist;
    private int playlistSize, playlistPosition;

    //Audio URL
    private String mediaFile;

    //Audio title and subtitle
    private String title, sub, img, currentTime, duration;

    public String getTitle() {
        return title;
    }

    public String getSub() {
        return sub;
    }

    private int resumePosition;


    //Handling incoming phone calls
    private boolean onGoingCall = false;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    public static final String Broadcast_PLAYER_PREPARED = "it.stream.streamit.PlayerPrepared";
    public static final String Buffering_Upadate = "it.stream.streamit.BufferingUpdate";
    public static final String New_Audio = "it.stream.streamit.NewAudio";
    public static final String Buffering = "it.stream.streamit.Buffering";
    public static final String buffering_End = "it.stream.streamit.bufferingEnd";
    public static final String Action_Play = "it.stream.streamit.ACTION_PLAY";
    public static final String Action_Pause = "it.stream.streamit.ACTION_PAUSE";

    /*
    Notification Stuff
     */

    NotificationManagerCompat notificationManagerCompat;

    enum Status {
        Playing,
        Paused,
        Loading
    }

    Bitmap bitmap;

    private void showNotif(Status status) {
        boolean Flag_Sticky = true;
        Intent intent;
        PendingIntent pendingIntent;
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_view);
        remoteViews.setTextViewText(R.id.notificationTitle, title);
        remoteViews.setTextViewText(R.id.notificationSubtitle, sub);
        switch (status) {
            case Playing:
                intent = new Intent(Action_Pause);
                pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
                remoteViews.setOnClickPendingIntent(R.id.notificationButton, pendingIntent);
                remoteViews.setViewVisibility(R.id.notificationProgress, View.GONE);
                remoteViews.setViewVisibility(R.id.notificationButton, View.VISIBLE);
                remoteViews.setImageViewResource(R.id.notificationButton, R.drawable.new_pause_icon);
                Flag_Sticky = true;
                break;
            case Paused:
                intent = new Intent(Action_Play);
                pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
                remoteViews.setOnClickPendingIntent(R.id.notificationButton, pendingIntent);
                remoteViews.setViewVisibility(R.id.notificationProgress, View.GONE);
                remoteViews.setViewVisibility(R.id.notificationButton, View.VISIBLE);
                remoteViews.setImageViewResource(R.id.notificationButton, R.drawable.new_play_arrow);
                Flag_Sticky = false;
                break;
            case Loading:
                remoteViews.setViewVisibility(R.id.notificationButton, View.GONE);
                remoteViews.setViewVisibility(R.id.notificationProgress, View.VISIBLE);
                Flag_Sticky = true;
                break;
        }


        new getBitmapFromURL().execute(img);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.ic_notifiction_icon)
                .setLargeIcon(bitmap)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContent(remoteViews)
                .setCustomContentView(remoteViews)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(Flag_Sticky);

        notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(100, mBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "default";
            String description = "The default notification channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private BroadcastReceiver notificationPlay = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resumeMedia();
        }
    };

    private void registerNotificationPlay() {
        IntentFilter intentFilter = new IntentFilter(this.Action_Play);
        registerReceiver(notificationPlay, intentFilter);
    }

    private BroadcastReceiver notificationPause = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };

    private void registerNotificationPause() {
        IntentFilter intentFilter = new IntentFilter(this.Action_Pause);
        registerReceiver(notificationPause, intentFilter);
    }

    /*/
    Notification Stuff
     */

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        registerNotificationPlay();
        registerNotificationPause();
        createNotificationChannel();

        try {
            //An audio file is passed to service through push extra
            mediaFile = intent.getExtras().getString("media");
            title = intent.getExtras().getString("title");
            sub = intent.getExtras().getString("sub");
            img = intent.getExtras().getString("img");

            //Playlist
            String json = intent.getExtras().getString("playlist");
            Type type = new TypeToken<List<ListItem>>() {
            }.getType();
            Gson gson = new Gson();
            mPlaylist = gson.fromJson(json, type);

            playlistSize = intent.getExtras().getInt("size");
            playlistPosition = intent.getExtras().getInt("position");


        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (!requestAudioFocus()) {
            stopSelf();
        }

        //init media player
        if (mediaFile != null && mediaFile != "") {
            Intent intent1 = new Intent(New_Audio);
            intent1.putExtra("title", title);
            intent1.putExtra("sub", sub);
            intent1.putExtra("img", img);
            sendBroadcast(intent1);

            stopMedia();
            initMediaPlayer();
        }

        showNotif(Status.Loading);

        return super.onStartCommand(intent, flags, startId);
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == audioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //manage incoming phone calls during playback
        //pause media on incoming calls
        //resume on hangup
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY
        registerBecomingNoisyReceiver();
        //listen for new audio to play
        register_playNewAudio();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            //stopMedia();
            mediaPlayer.release();
            notificationManagerCompat.cancel(100);
        }
        removeAudioFocus();

        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(notificationPlay);
        unregisterReceiver(notificationPause);
    }

    @Override
    public void onAudioFocusChange(int i) {
        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                //Resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //lost the focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //Lost focus for short time but have to stop playback
                //we don't release the media player because playback is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //lost focus for a short time but it's ok to keep playing
                //at an attenauted level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onBufferingUpdate(android.media.MediaPlayer mediaPlayer, int i) {
        Intent intent = new Intent(Buffering_Upadate);
        intent.putExtra("status", i);
        sendBroadcast(intent);
    }

    @Override
    public void onCompletion(android.media.MediaPlayer mediaPlayer) {
        if (!(playlistPosition == playlistSize - 1)) {
            playlistPosition++;
            title = mPlaylist.get(playlistPosition).getTitle();
            img = mPlaylist.get(playlistPosition).getImageUrl();
            mediaFile = mPlaylist.get(playlistPosition).getURL();
            String st = mPlaylist.get(playlistPosition).getArtist();
            st += " | ";
            st += mPlaylist.get(playlistPosition).getYear();
            sub = st;

            Intent intent1 = new Intent(New_Audio);
            intent1.putExtra("title", title);
            intent1.putExtra("sub", sub);
            intent1.putExtra("img", img);
            sendBroadcast(intent1);

            stopMedia();
            //mediaPlayer.reset();
            mediaPlayer.release();
            initMediaPlayer();

            showNotif(Status.Loading);
        }
    }

    @Override
    public boolean onError(android.media.MediaPlayer mediaPlayer, int i, int i1) {
        switch (i) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(android.media.MediaPlayer mediaPlayer, int i, int i1) {
        switch (i) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Intent intent = new Intent(Buffering);
                sendBroadcast(intent);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Intent intent1 = new Intent(buffering_End);
                sendBroadcast(intent1);
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(android.media.MediaPlayer mediaPlayer) {
        //Invoked when media source is ready for playback
        playMedia();
        //Tell main activity MediaPlayer is ready to play audio
        Intent broadcastIntent = new Intent(Broadcast_PLAYER_PREPARED);
        broadcastIntent.putExtra("title", title);
        broadcastIntent.putExtra("sub", sub);
        broadcastIntent.putExtra("img", img);
        sendBroadcast(broadcastIntent);

        showNotif(Status.Playing);
    }

    @Override
    public void onSeekComplete(android.media.MediaPlayer mediaPlayer) {

    }

    /**
     * Broadcast receiver
     */

    //becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    /**
     * Handle Incoming Calls
     */

    private void callStateListener() {
        //Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exist or phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            onGoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        //Phone idle. Start playing
                        if (mediaPlayer != null) {
                            if (onGoingCall) {
                                onGoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        //Register the listener with the telephony manager
        //listen for changes to device call state
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * play new audio broadcast receiver
     */

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //an audio file is passed to the service through putExtra
                mediaFile = intent.getExtras().getString("media");
                title = intent.getExtras().getString("title");
                sub = intent.getExtras().getString("sub");
                img = intent.getExtras().getString("img");

                //Playlist
                String json = intent.getExtras().getString("playlist");
                Type type = new TypeToken<List<ListItem>>() {
                }.getType();
                Gson gson = new Gson();
                mPlaylist = gson.fromJson(json, type);

                playlistSize = intent.getExtras().getInt("size");
                playlistPosition = intent.getExtras().getInt("position");

            } catch (NullPointerException e) {
                stopSelf();
            }

            Intent intent1 = new Intent(New_Audio);
            intent1.putExtra("title", title);
            intent1.putExtra("sub", sub);
            intent1.putExtra("img", img);
            sendBroadcast(intent1);

            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();

            showNotif(Status.Loading);
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    /**
     * media player
     */


    public void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //set up media player event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that media player is not pointing to another resource
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            //set the data source to mediaFile location
            mediaPlayer.setDataSource(mediaFile);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopMedia() {
        if (mediaPlayer == null) return;
        else {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();

            showNotif(Status.Paused);

            Intent intent = new Intent(Action_Pause);
            sendBroadcast(intent);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("playing", false);
            editor.commit();
        }
    }

    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();

            showNotif(Status.Playing);

            Intent intent = new Intent(Action_Play);
            sendBroadcast(intent);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("playing", true);
            editor.commit();

        }
    }

    public void seek(int i) {
        resumePosition = i;
        mediaPlayer.seekTo(resumePosition);
    }

    public void next() {
        if (!(playlistPosition == playlistSize - 1)) {
            playlistPosition++;
            title = mPlaylist.get(playlistPosition).getTitle();
            img = mPlaylist.get(playlistPosition).getImageUrl();
            mediaFile = mPlaylist.get(playlistPosition).getURL();
            String st = mPlaylist.get(playlistPosition).getArtist();
            st += " | ";
            st += mPlaylist.get(playlistPosition).getYear();
            sub = st;

            Intent intent1 = new Intent(New_Audio);
            intent1.putExtra("title", title);
            intent1.putExtra("sub", sub);
            intent1.putExtra("img", img);
            sendBroadcast(intent1);

            stopMedia();
            //mediaPlayer.reset();
            mediaPlayer.release();
            initMediaPlayer();

            showNotif(Status.Loading);
        }
    }

    public void previous() {
        if (!(playlistPosition == 0)) {
            playlistPosition--;
            title = mPlaylist.get(playlistPosition).getTitle();
            img = mPlaylist.get(playlistPosition).getImageUrl();
            mediaFile = mPlaylist.get(playlistPosition).getURL();
            String st = mPlaylist.get(playlistPosition).getArtist();
            st += " | ";
            st += mPlaylist.get(playlistPosition).getYear();
            sub = st;

            Intent intent1 = new Intent(New_Audio);
            intent1.putExtra("title", title);
            intent1.putExtra("sub", sub);
            intent1.putExtra("img", img);
            sendBroadcast(intent1);

            stopMedia();
            //mediaPlayer.reset();
            mediaPlayer.release();
            initMediaPlayer();

            showNotif(Status.Loading);
        }
    }


    public int getDuration() {
        int du = mediaPlayer.getDuration();
        return du;
    }

    public int getCurrentPosition() {
        int cp = mediaPlayer.getCurrentPosition();
        return cp;
    }

    public String getImg() {
        return img;
    }

    public boolean getPlayerStatus() {
        boolean playerStatus = (mediaPlayer != null);
        return playerStatus;
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private class getBitmapFromURL extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                bitmap = Bitmap.createScaledBitmap(myBitmap, 64, 64, false);
                return myBitmap;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
