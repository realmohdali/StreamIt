package it.stream.streamit.backgroundService;

import android.annotation.SuppressLint;
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
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
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
import java.util.ArrayList;
import java.util.List;

import it.stream.streamit.database.FavoriteManagement;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.MainActivity;
import it.stream.streamit.R;
import it.stream.streamit.database.RecentManagement;

import static android.media.AudioManager.*;
import static it.stream.streamit.adapters.AlbumAdapter.Broadcast_PLAY_NEW_AUDIO;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.*;

public class MediaService extends Service
        implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        OnAudioFocusChangeListener,
        MediaPlayer.OnInfoListener {

    //class Variables

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    //Playlist
    private List<ListItem> mPlaylist;
    private int playlistPosition;
    boolean isFav;

    //Audio URL
    private String mediaFile;

    //Handling Call state
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean Playing_Before_call;

    //Audio title img sub
    private String title, sub, img, year, artist;
    private boolean isLoading, haveTrack, isPlaying;
    private int duration;

    //Resume Position
    private int resumePosition;

    //Handle Seek Update
    private Handler handler;
    private boolean running = false;

    //Loop strings
    private static final int noLoop = 1;
    private static final int loopAll = 2;
    private static final int loopOne = 3;

    //player constants
    public static final String Broadcast_PLAYER_PREPARED = "it.stream.streamit.PlayerPrepared";
    public static final String Buffering_Update = "it.stream.streamit.BufferingUpdate";
    public static final String New_Audio = "it.stream.streamit.NewAudio";
    public static final String Buffering = "it.stream.streamit.Buffering";
    public static final String buffering_End = "it.stream.streamit.bufferingEnd";

    private SQLiteDatabase db, recent;

    //parse playlist
    private String json = "";

    //Notification variables
    enum Status {
        Playing,
        Paused,
        Loading
    }

    Bitmap bitmap;

    //______________________________________________________________________________________________

    //Lifecycle methods

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        db = openOrCreateDatabase("favorite", MODE_PRIVATE, null);
        recent = openOrCreateDatabase("recent", MODE_PRIVATE, null);
        mPlaylist = new ArrayList<>();
        try {
            readData();

            if (intent.getExtras().getBoolean("isPlayList")) {
                json = intent.getExtras().getString("playlist");
                Type type = new TypeToken<List<ListItem>>() {
                }.getType();
                Gson gson = new Gson();
                mPlaylist = gson.fromJson(json, type);
                playlistPosition = intent.getExtras().getInt("pos");
                RecentManagement addToRecent = new RecentManagement(recent);
                addToRecent.add(mPlaylist.get(0));
            } else {
                ListItem li = new ListItem(title, artist, img, mediaFile, year);
                mPlaylist.add(li);
                RecentManagement addToRecent = new RecentManagement(recent);
                addToRecent.add(li);

                Gson gson = new Gson();
                json = gson.toJson(mPlaylist);
                playlistPosition = 0;
            }


        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (!requestAudioFocus()) {
            stopSelf();
        }

        //init media player
        if (mediaFile != null && !mediaFile.equals("")) {
            isLoading = true;
            haveTrack = true;
            writeData();

            sendNewAudioBroadcast();

            stopMedia();
            initMediaPlayer();
        }

        showNotification(MediaService.Status.Loading);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Playing_Before_call = false;

        registerNotificationPlay();
        registerNotificationPause();
        callStateListener();
        registerBecomingNoisyReceiver();
        register_playNewAudio();
        createNotificationChannel();
        registerResume();
        registerPause();
        registerNext();
        registerPrev();
        registerStop();
        registerSeek();
        registerRemoveTrack();
        registerAddToPlaylist();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("serviceRunning", true);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            stopForeground(true);
        }
        removeAudioFocus();

        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(notificationPlay);
        unregisterReceiver(notificationPause);
        unregisterReceiver(resume);
        unregisterReceiver(pause);
        unregisterReceiver(next);
        unregisterReceiver(prev);
        unregisterReceiver(stop);
        unregisterReceiver(seek);
        unregisterReceiver(removeTrack);
        unregisterReceiver(addToPlayList);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        if (running) {
            handler.removeCallbacks(seekUpdate);
        }

        clearData();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    //Lifecycle methods end
    //______________________________________________________________________________________________

    //Listeners

    @Override
    public void onAudioFocusChange(int i) {
        switch (i) {
            case AUDIOFOCUS_GAIN:
                //Resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) {
                    if (Playing_Before_call) {
                        resumeMedia();
                    }
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AUDIOFOCUS_LOSS:
                //lost the focus for an unbounded amount of time: stop playback
                // and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer = null;
                break;
            case AUDIOFOCUS_LOSS_TRANSIENT:
                //Lost focus for short time but have to stop playback
                //we don't release the media player because playback is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //lost focus for a short time but it's ok to keep playing
                //at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.3f,
                        0.3f);
                break;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        Intent intent = new Intent(Buffering_Update);
        intent.putExtra("status", i);
        sendBroadcast(intent);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int loop = sp.getInt("loop", 1);
        if (loop == loopOne) {
            mediaPlayer.seekTo(0);
            playMedia();
            isPlaying = true;
        }
        if (loop == loopAll) {
            if (!(playlistPosition == mPlaylist.size() - 1)) {
                playlistPosition++;
                title = mPlaylist.get(playlistPosition).getTitle();
                img = mPlaylist.get(playlistPosition).getImageUrl();
                mediaFile = mPlaylist.get(playlistPosition).getURL();
                String st = mPlaylist.get(playlistPosition).getArtist();
                st += " | ";
                st += mPlaylist.get(playlistPosition).getYear();
                sub = st;

                isLoading = true;

                writeData();

                ListItem li = new ListItem(title, artist, img, mediaFile, year);
                RecentManagement addToRecent = new RecentManagement(recent);
                addToRecent.add(li);

                sendNewAudioBroadcast();

                stopMedia();
                mediaPlayer.release();
                initMediaPlayer();

                showNotification(MediaService.Status.Loading);
            } else if (playlistPosition == mPlaylist.size() - 1) {
                playlistPosition = 0;
                title = mPlaylist.get(playlistPosition).getTitle();
                img = mPlaylist.get(playlistPosition).getImageUrl();
                mediaFile = mPlaylist.get(playlistPosition).getURL();
                String st = mPlaylist.get(playlistPosition).getArtist();
                st += " | ";
                st += mPlaylist.get(playlistPosition).getYear();
                sub = st;

                isLoading = true;

                writeData();

                ListItem li = new ListItem(title, artist, img, mediaFile, year);
                RecentManagement addToRecent = new RecentManagement(recent);
                addToRecent.add(li);

                sendNewAudioBroadcast();

                stopMedia();
                mediaPlayer.release();
                initMediaPlayer();

                showNotification(MediaService.Status.Loading);
            }
        }
        if (loop == noLoop) {
            if (!(playlistPosition == mPlaylist.size() - 1)) {
                playlistPosition++;
                title = mPlaylist.get(playlistPosition).getTitle();
                img = mPlaylist.get(playlistPosition).getImageUrl();
                mediaFile = mPlaylist.get(playlistPosition).getURL();
                String st = mPlaylist.get(playlistPosition).getArtist();
                st += " | ";
                st += mPlaylist.get(playlistPosition).getYear();
                sub = st;

                isLoading = true;

                writeData();

                ListItem li = new ListItem(title, artist, img, mediaFile, year);
                RecentManagement addToRecent = new RecentManagement(recent);
                addToRecent.add(li);

                sendNewAudioBroadcast();

                stopMedia();
                mediaPlayer.release();
                initMediaPlayer();

                showNotification(MediaService.Status.Loading);
            } else {
                Intent killPlayer = new Intent(ACTION_STOP);
                sendBroadcast(killPlayer);
                stopForeground(true);
            }
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        switch (i) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Intent intent = new Intent(Buffering);
                sendBroadcast(intent);
                showNotification(Status.Loading);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Intent intent1 = new Intent(buffering_End);
                sendBroadcast(intent1);
                showNotification(Status.Playing);
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //Invoked when media source is ready for playback
        playMedia();

        isLoading = false;
        isPlaying = true;
        duration = mediaPlayer.getDuration();

        writeData();

        //Tell main activity MediaPlayer is ready to play audio
        Intent broadcastIntent = new Intent(Broadcast_PLAYER_PREPARED);
        sendBroadcast(broadcastIntent);

        handler = new Handler();
        handler.postDelayed(seekUpdate, 500);
        running = true;

        showNotification(Status.Playing);
    }

    //Listeners End
    //______________________________________________________________________________________________

    //Notification stuff

    private void showNotification(Status status) {
        boolean Flag_Sticky = true;
        Intent intent;
        PendingIntent pendingIntent;
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_view);
        remoteViews.setTextViewText(R.id.notificationTitle, title);
        remoteViews.setTextViewText(R.id.notificationSubtitle, sub);
        switch (status) {
            case Playing:
                intent = new Intent(ACTION_PAUSE);
                pendingIntent = PendingIntent.getBroadcast(this, 0,
                        intent, 0);
                remoteViews.setOnClickPendingIntent(R.id.notificationButton, pendingIntent);
                remoteViews.setViewVisibility(R.id.notificationProgress, View.GONE);
                remoteViews.setViewVisibility(R.id.notificationButton, View.VISIBLE);
                remoteViews.setImageViewResource(R.id.notificationButton,
                        R.drawable.new_pause_icon);
                Flag_Sticky = true;
                break;
            case Paused:
                intent = new Intent(ACTION_PLAY);
                pendingIntent = PendingIntent.getBroadcast(this, 0,
                        intent, 0);
                remoteViews.setOnClickPendingIntent(R.id.notificationButton, pendingIntent);
                remoteViews.setViewVisibility(R.id.notificationProgress, View.GONE);
                remoteViews.setViewVisibility(R.id.notificationButton, View.VISIBLE);
                remoteViews.setImageViewResource(R.id.notificationButton,
                        R.drawable.new_play_arrow);
                Flag_Sticky = false;
                break;
            case Loading:
                remoteViews.setViewVisibility(R.id.notificationButton, View.GONE);
                remoteViews.setViewVisibility(R.id.notificationProgress, View.VISIBLE);
                Flag_Sticky = true;
                break;
        }


        new MediaService.getBitmapFromURL().execute(img);

        Intent resumeIntent = new Intent(this, MainActivity.class);
        resumeIntent.setAction(Intent.ACTION_MAIN);
        resumeIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent resumePendingIntent = PendingIntent.getActivity(this,
                0, resumeIntent, 0);

        Intent stopIntent = new Intent(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this,
                0, stopIntent, 0);
        Intent nextIntent = new Intent(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this,
                0, nextIntent, 0);
        Intent prevIntent = new Intent(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this,
                0, prevIntent, 0);

        NotificationCompat.Builder mBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mBuilder = new NotificationCompat.Builder(this,
                    getResources().getString(R.string.notification_channel_id))
                    .setSmallIcon(R.drawable.ic_notifiction_icon)
                    .setLargeIcon(bitmap)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setContent(remoteViews)
                    .setCustomContentView(remoteViews)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setOngoing(Flag_Sticky)
                    .setContentIntent(resumePendingIntent)
                    .addAction(R.drawable.ic_stop, getString(R.string.stop), stopPendingIntent)
                    .addAction(R.drawable.ic_skip_previous_white_24dp, getString(R.string.prev), prevPendingIntent)
                    .addAction(R.drawable.ic_skip_next_white_24dp, getString(R.string.next), nextPendingIntent);
        }

        assert mBuilder != null;
        startForeground(100, mBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "Matam | Labbaik Ya Hussain";
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new
                    NotificationChannel(getResources().getString(R.string.notification_channel_id),
                    getResources().getString(R.string.notification_channel_name), importance);

            channel.setDescription(description);
            channel.setLightColor(Color.GREEN);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
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
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAY);
        registerReceiver(notificationPlay, intentFilter);
    }

    private BroadcastReceiver notificationPause = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };

    private void registerNotificationPause() {
        IntentFilter intentFilter = new IntentFilter(ACTION_PAUSE);
        registerReceiver(notificationPause, intentFilter);
    }

    //Notification Stuff end
    //______________________________________________________________________________________________

    //BroadCast send

    private void sendNewAudioBroadcast() {
        isPlaying = false;
        isLoading = true;
        writeData();

        Intent intent = new Intent(New_Audio);
        intent.putExtra("title", title);
        intent.putExtra("sub", sub);
        intent.putExtra("img", img);
        intent.putExtra("url", mediaFile);
        intent.putExtra("fav", isFav);
        intent.putExtra("playlist", json);
        sendBroadcast(intent);
    }

    private Runnable seekUpdate = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer.isPlaying()) {
                Intent intent = new Intent(SEEK_UPDATE);
                int currentPosition = mediaPlayer.getCurrentPosition();
                intent.putExtra("currentPosition", currentPosition);
                sendBroadcast(intent);
            }
            handler.postDelayed(this, 500);
        }
    };

    //Broadcast send end
    //______________________________________________________________________________________________

    //Broadcast receiver

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                pauseMedia();
                                Playing_Before_call = true;
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (Playing_Before_call) {
                            resumeMedia();
                        }
                        break;
                }
            }
        };
        assert telephonyManager != null;
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {

                readData();

                if (intent.getExtras().getBoolean("isPlayList")) {
                    for (int i = 0; i < mPlaylist.size(); i++) {
                        mPlaylist.remove(i);
                    }
                    json = intent.getExtras().getString("playlist");
                    Type type = new TypeToken<List<ListItem>>() {
                    }.getType();
                    Gson gson = new Gson();
                    mPlaylist = gson.fromJson(json, type);
                    playlistPosition = intent.getExtras().getInt("pos");
                    RecentManagement addToRecent = new RecentManagement(recent);
                    addToRecent.add(mPlaylist.get(0));
                } else {
                    boolean exists = false;
                    int position = -1;

                    for (int i = 0; i < mPlaylist.size(); i++) {
                        String url = mPlaylist.get(i).getURL();
                        if (url.equals(mediaFile)) {
                            exists = true;
                            position = i;
                            break;
                        }
                    }

                    if (exists) {
                        playlistPosition = position;
                        Gson gson = new Gson();
                        json = gson.toJson(mPlaylist);
                    } else {
                        ListItem li = new ListItem(title, artist, img, mediaFile, year);
                        mPlaylist.add(li);

                        RecentManagement addToRecent = new RecentManagement(recent);
                        addToRecent.add(li);

                        Gson gson = new Gson();
                        json = gson.toJson(mPlaylist);

                        playlistPosition = mPlaylist.size() - 1;
                    }
                }

            } catch (NullPointerException e) {
                stopSelf();
            }

            if (running) {
                handler.removeCallbacks(seekUpdate);
            }

            isLoading = true;
            haveTrack = true;
            writeData();

            sendNewAudioBroadcast();

            stopMedia();
            initMediaPlayer();

            showNotification(Status.Loading);
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    //Broadcast receiver end
    //______________________________________________________________________________________________

    //Audio focus

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        int result = audioManager.requestAudioFocus(this, STREAM_MUSIC,
                AUDIOFOCUS_GAIN);
        return result == AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean removeAudioFocus() {
        return AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    //Audio focus end
    //______________________________________________________________________________________________

    //Media player functions

    public void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //set up media player event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that media player is not pointing to another resource
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(STREAM_MUSIC);
        try {
            //set the data source to mediaFile location
            mediaPlayer.setDataSource(mediaFile);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.prepareAsync();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            stopForeground(true);
        }
    }

    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            Playing_Before_call = false;
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();

            showNotification(Status.Paused);

            Intent intent = new Intent(ACTION_PAUSE);
            sendBroadcast(intent);

            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("playing", false);
            editor.apply();
        }
    }

    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();

            Playing_Before_call = true;

            showNotification(Status.Playing);

            Intent intent = new Intent(ACTION_PLAY);
            sendBroadcast(intent);

            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("playing", true);
            editor.apply();

        }
    }

    public void seek(int i) {
        resumePosition = i;
        mediaPlayer.seekTo(resumePosition);
    }

    public void next() {
        if (!(playlistPosition == mPlaylist.size() - 1)) {
            playlistPosition++;
            title = mPlaylist.get(playlistPosition).getTitle();
            img = mPlaylist.get(playlistPosition).getImageUrl();
            mediaFile = mPlaylist.get(playlistPosition).getURL();
            String st = mPlaylist.get(playlistPosition).getArtist();
            st += " | ";
            st += mPlaylist.get(playlistPosition).getYear();
            sub = st;

            writeData();

            ListItem li = new ListItem(title, artist, img, mediaFile, year);
            RecentManagement addToRecent = new RecentManagement(recent);
            addToRecent.add(li);

            sendNewAudioBroadcast();

            stopMedia();
            mediaPlayer.release();
            initMediaPlayer();

            showNotification(Status.Loading);
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

            writeData();

            ListItem li = new ListItem(title, artist, img, mediaFile, year);
            RecentManagement addToRecent = new RecentManagement(recent);
            addToRecent.add(li);

            sendNewAudioBroadcast();

            stopMedia();
            mediaPlayer.release();
            initMediaPlayer();

            showNotification(Status.Loading);
        }
    }

    //Media player functions end
    //______________________________________________________________________________________________

    //Media player control receiver

    private BroadcastReceiver resume = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                resumeMedia();
            }
        }
    };

    private void registerResume() {
        IntentFilter filter = new IntentFilter(ACTION_PLAY);
        registerReceiver(resume, filter);
    }

    private BroadcastReceiver pause = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                pauseMedia();
            }
        }
    };

    private void registerPause() {
        IntentFilter filter = new IntentFilter(ACTION_PAUSE);
        registerReceiver(pause, filter);
    }

    private BroadcastReceiver next = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer != null) {
                next();
            }
        }
    };

    private void registerNext() {
        IntentFilter filter = new IntentFilter(ACTION_NEXT);
        registerReceiver(next, filter);
    }

    private BroadcastReceiver prev = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer != null) {
                previous();
            }
        }
    };

    private void registerPrev() {
        IntentFilter filter = new IntentFilter(ACTION_PREV);
        registerReceiver(prev, filter);
    }

    private BroadcastReceiver stop = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clearData();
            stopSelf();
        }
    };

    private void registerStop() {
        IntentFilter filter = new IntentFilter(ACTION_STOP);
        registerReceiver(stop, filter);
    }

    private BroadcastReceiver seek = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int i = intent.getExtras().getInt("pos");
            seek(i);
        }
    };

    private void registerSeek() {
        IntentFilter filter = new IntentFilter(ACTION_SEEK);
        registerReceiver(seek, filter);
    }

    private BroadcastReceiver removeTrack = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int i = intent.getExtras().getInt("i");
            mPlaylist.remove(i);
            Gson gson = new Gson();
            json = gson.toJson(mPlaylist);
            writeData();
        }
    };

    private void registerRemoveTrack() {
        IntentFilter filter = new IntentFilter(REMOVE_ITEM);
        registerReceiver(removeTrack, filter);
    }

    private BroadcastReceiver addToPlayList = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String title, artist, img, mediaFile, year;
            title = intent.getExtras().getString("title");
            artist = intent.getExtras().getString("artist");
            img = intent.getExtras().getString("img");
            mediaFile = intent.getExtras().getString("url");
            year = intent.getExtras().getString("year");

            boolean exists = false;

            for (int i = 0; i < mPlaylist.size(); i++) {
                String url = mPlaylist.get(i).getURL();
                if (url.equals(mediaFile)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                ListItem li = new ListItem(title, artist, img, mediaFile, year);
                mPlaylist.add(li);

                Gson gson = new Gson();
                json = gson.toJson(mPlaylist);
                writeData();
            }

            Intent intent1 = new Intent(PLAYLIST_UPDATE);
            sendBroadcast(intent1);
        }
    };

    private void registerAddToPlaylist() {
        IntentFilter filter = new IntentFilter(ADD_TO_PLAYLIST);
        registerReceiver(addToPlayList, filter);
    }


    //Media player control receiver end
    //______________________________________________________________________________________________

    //Data operations

    private void readData() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mediaFile = sp.getString("media", "");
        title = sp.getString("title", "");
        sub = sp.getString("sub", "");
        img = sp.getString("img", "");
        year = sp.getString("year", "");
        artist = sp.getString("artist", "");
    }

    private void writeData() {
        FavoriteManagement favoriteManagement = new FavoriteManagement(mediaFile, db);
        isFav = favoriteManagement.alreadyExists();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("title", title);
        editor.putString("sub", sub);
        editor.putString("img", img);
        editor.putString("url", mediaFile);
        editor.putString("playlist", json);
        editor.putBoolean("fav", isFav);
        editor.putBoolean("loading", isLoading);
        editor.putBoolean("playing", isPlaying);
        editor.putBoolean("haveTrack", haveTrack);
        editor.putInt("intDuration", duration);
        editor.putInt("playerPosition", playlistPosition);
        editor.putBoolean("serviceRunning", true);
        editor.apply();
    }

    private void clearData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("title", "Track Title");
        editor.putString("sub", "Artist | Year");
        editor.putString("img", "");
        editor.putBoolean("loading", false);
        editor.putBoolean("playing", false);
        editor.putBoolean("haveTrack", false);
        editor.putInt("currentTime", 0);
        editor.putString("currentStringTime", "00:00");
        editor.putInt("intDuration", 0);
        editor.putString("duration", "00:00");
        editor.putBoolean("fav", false);
        editor.putString("playlist", "");
        editor.putBoolean("serviceRunning", false);

        editor.apply();
    }

    //Data operation end
    //______________________________________________________________________________________________

    //Default Method
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //Default Method end
    //______________________________________________________________________________________________

    //inner class for loading bitmaps from URL
    @SuppressLint("StaticFieldLeak")
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
                bitmap = Bitmap.createScaledBitmap(myBitmap, 64, 64,
                        false);
                return myBitmap;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
