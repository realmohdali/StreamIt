package it.stream.streamit;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private List<ListItem> mListItems;

    private RecyclerView mArtistView;
    private RecyclerView.Adapter mArtistAdapter;
    private List<ArtistList> mArtistList;

    private RecyclerView mYearView;
    private RecyclerView.Adapter mYearViewAdapter;
    private List<YearList> mYearList;

    private MediaPlayerService mediaPlayer;
    private boolean playing;
    private boolean serviceBound;

    //BottomSheetStuff
    private ImageButton pb;
    private ImageButton con;
    private TextView ct, st, tt, ts, cp, du;
    private ImageView iv1, iv2;
    private boolean expanded;
    private SeekBar mSeekBar;
    private ProgressBar loading, loadingExp;
    private int intDuration, currentTime;
    private boolean isLoading, haveTrack;
    private String duration, trackTitle, trackSub, trackImg, currentStringTime;

    private Handler mHandler;
    private boolean running = false;

    BottomSheetBehavior sheetBehavior;

    //URLs
    private static final String URL1 = "http://realmohdali.000webhostapp.com/streamIt/php_modules/whatsNewHome.php";
    private static final String URL2 = "http://realmohdali.000webhostapp.com/streamIt/php_modules/artistHome.php";
    private static final String URL3 = "http://realmohdali.000webhostapp.com/streamIt/php_modules/yearHome.php";

    //Broadcast Strings
    private static final String Action_Play = "it.stream.streamit.ACTION_PLAY";
    private static final String Action_Pause = "it.stream.streamit.ACTION_PAUSE";

    private boolean doubleBackToExitPressedOnce = false;

    //Activity Life Cycle start

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clearData();
        loadActivity();
        if (isOnline()) {
            loadBottomSheet();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        trackTitle = preferences.getString("title", "");
        trackSub = preferences.getString("sub", "");
        trackImg = preferences.getString("img", "");
        isLoading = preferences.getBoolean("loading", false);
        playing = preferences.getBoolean("playing", false);
        haveTrack = preferences.getBoolean("haveTrack", false);
        currentTime = preferences.getInt("currentTime", 0);
        currentStringTime = preferences.getString("currentStringTime", "");
        duration = preferences.getString("duration", "");
        intDuration = preferences.getInt("intDuration", 0);
        if (isOnline()) {
            loadPlayer();
        }
    }

    @Override
    protected void onPause() {
        writeData();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(newAudio);
        unregisterReceiver(playerPrepared);
        unregisterReceiver(bufferingUpdate);
        unregisterReceiver(buffering);
        unregisterReceiver(bufferingEnd);
        unregisterReceiver(paused);
        unregisterReceiver(resume);
        clearData();
        if (running) {
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
        unbindService(serviceConnection);
        super.onDestroy();
    }

    //Activity life cycle end

    //Methods to load data on screen

    private void loadActivity() {
        if (isOnline()) {

            registerPlayerPrepared();
            registerBufferingUpdate();
            registerNewAudio();
            registerBuffering();
            registerBufferingEnd();
            registerPaused();
            registerResume();

            //Bottom Sheet Stuff
            pb = findViewById(R.id.play);
            con = findViewById(R.id.control);
            ct = findViewById(R.id.currentTitle);
            st = findViewById(R.id.subtitle);
            tt = findViewById(R.id.tackTitle);
            ts = findViewById(R.id.trackSub);
            iv1 = findViewById(R.id.img);
            iv2 = findViewById(R.id.albumArt);
            cp = findViewById(R.id.cTime);
            du = findViewById(R.id.eTime);
            mSeekBar = findViewById(R.id.seekBar);
            loading = findViewById(R.id.loading);
            loadingExp = findViewById(R.id.loadingExp);
            intDuration = 0;
            currentTime = 0;
            isLoading = false;
            trackTitle = "Track Title";
            trackSub = "Artist | year";
            trackImg = "";
            haveTrack = false;
            currentStringTime = "00:00";

            Intent intent = new Intent(this, MediaPlayerService.class);
            bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

            LinearLayoutManager mLayoutManger = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

            mRecyclerView = findViewById(R.id.rv);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(mLayoutManger);

            mArtistView = findViewById(R.id.artistView);
            mArtistView.setHasFixedSize(true);
            mArtistView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            mYearView = findViewById(R.id.yearView);
            mYearView.setHasFixedSize(true);
            mYearView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));


            mListItems = new ArrayList<>();
            mArtistList = new ArrayList<>();
            mYearList = new ArrayList<>();

            loadData();
        } else {
            Snackbar sb = Snackbar.make(findViewById(R.id.mainLayout), "You are offline", Snackbar.LENGTH_INDEFINITE);
            sb.setAction("Try Again", new TryAgain());
            sb.show();

        }
    }

    private void loadBottomSheet() {

        pb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPause(view);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (serviceBound && b) {
                    mediaPlayer.seek(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        con.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPause(view);
            }
        });

        ImageButton back = findViewById(R.id.back);
        ImageButton next = findViewById(R.id.next);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (haveTrack) {
                    mediaPlayer.previous();
                }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (haveTrack) {
                    mediaPlayer.next();
                }
            }
        });

        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        expanded = false;

        //State change listener
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        getSupportActionBar().setDisplayShowHomeEnabled(true);
                        getSupportActionBar().setTitle(R.string.playerTitle);
                        expanded = true;
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        getSupportActionBar().setDisplayShowHomeEnabled(false);
                        getSupportActionBar().setTitle(R.string.app_name);
                        expanded = false;
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float x = 1 - slideOffset;
                findViewById(R.id.btmSheet).setAlpha(x);

            }
        });
    }

    private void loadData() {
        loadNew();
        loadArtist();
        loadYear();
    }

    private void loadNew() {
        final ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Loading Data...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        StringRequest mStringRequest = new StringRequest(Request.Method.GET,
                URL1,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            mProgressDialog.dismiss();
                            JSONArray mJsonArray = new JSONArray(response);

                            for (int i = 0; i < mJsonArray.length(); i++) {
                                JSONObject mJsonObject = mJsonArray.getJSONObject(i);

                                String title = mJsonObject.getString("title");
                                String artist = mJsonObject.getString("artist");
                                String year = mJsonObject.getString("year");
                                String link = "http://realmohdali.000webhostapp.com/streamIt/";
                                link += mJsonObject.getString("url");
                                String image = "http://realmohdali.000webhostapp.com/streamIt/";
                                image += mJsonObject.getString("image");

                                ListItem li = new ListItem(title, artist, image, link, year);
                                mListItems.add(li);
                            }

                            mAdapter = new RecentHomeAdapter(mListItems, getApplicationContext());
                            mRecyclerView.setAdapter(mAdapter);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        RequestQueue mRequest = Volley.newRequestQueue(getApplicationContext());
        mRequest.add(mStringRequest);
    }

    private void loadArtist() {
        StringRequest mStringRequest = new StringRequest(Request.Method.GET,
                URL2,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String image = "http://realmohdali.000webhostapp.com/streamIt/";
                                image += jsonObject.getString("image");
                                String artist = jsonObject.getString("artist");

                                ArtistList li = new ArtistList(artist, image);
                                mArtistList.add(li);
                            }
                            mArtistAdapter = new ArtistHomeAdapter(mArtistList, getApplicationContext());
                            mArtistView.setAdapter(mArtistAdapter);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        RequestQueue mRequest = Volley.newRequestQueue(getApplicationContext());
        mRequest.add(mStringRequest);
    }

    private void loadYear() {
        StringRequest mStringRequest = new StringRequest(Request.Method.GET,
                URL3,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String year = jsonObject.getString("year");
                                String image = "http://realmohdali.000webhostapp.com/streamIt/";
                                image += jsonObject.getString("image");

                                YearList li = new YearList(year, image);
                                mYearList.add(li);
                            }

                            mYearViewAdapter = new YearHomeAdapter(getApplicationContext(), mYearList);
                            mYearView.setAdapter(mYearViewAdapter);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
        RequestQueue mRequest = Volley.newRequestQueue(getApplicationContext());
        mRequest.add(mStringRequest);
    }

    private void clearData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
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

        editor.apply();
    }

    //Methods to load data on screen end

    //Broadcast Receivers

    private BroadcastReceiver playerPrepared = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            duration = "";
            int millis = mediaPlayer.getDuration();
            int seconds = (millis / 1000) % 60;
            long minutes = (millis / 1000 - seconds) / 60;

            if (minutes < 10) {
                duration += "0";
            }
            duration += Long.toString(minutes);
            duration += ":";
            if (seconds < 10) {
                duration += "0";
            }
            duration += Integer.toString(seconds);

            intDuration = mediaPlayer.getDuration();

            playing = true;

            isLoading = false;

            loadPlayer();
            writeData();
        }
    };

    private void registerPlayerPrepared() {
        IntentFilter filter = new IntentFilter(MediaPlayerService.Broadcast_PLAYER_PREPARED);
        registerReceiver(playerPrepared, filter);
    }

    private BroadcastReceiver newAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            trackTitle = intent.getExtras().getString("title");
            trackSub = intent.getExtras().getString("sub");
            trackImg = intent.getExtras().getString("img");

            isLoading = true;
            haveTrack = true;

            loadPlayer();
            writeData();
        }
    };

    private void registerNewAudio() {
        IntentFilter filter = new IntentFilter(MediaPlayerService.New_Audio);
        registerReceiver(newAudio, filter);
    }

    private BroadcastReceiver bufferingUpdate = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getExtras().getInt("status");
            double ratio = status / 100.0;
            int bufferingLevel = (int) (mSeekBar.getMax() * ratio);
            mSeekBar.setSecondaryProgress(bufferingLevel);
        }
    };

    private void registerBufferingUpdate() {
        IntentFilter filter = new IntentFilter(MediaPlayerService.Buffering_Upadate);
        registerReceiver(bufferingUpdate, filter);
    }

    private BroadcastReceiver buffering = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isLoading = true;
            loadPlayer();
        }
    };

    private void registerBuffering() {
        IntentFilter filter = new IntentFilter(MediaPlayerService.Buffering);
        registerReceiver(buffering, filter);
    }

    private BroadcastReceiver bufferingEnd = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isLoading = false;
            loadPlayer();
        }
    };

    private void registerBufferingEnd() {
        IntentFilter filter = new IntentFilter(MediaPlayerService.buffering_End);
        registerReceiver(bufferingEnd, filter);
    }

    private BroadcastReceiver paused = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playing = false;
            loadPlayer();
        }
    };

    private void registerPaused() {
        IntentFilter filter = new IntentFilter(Action_Pause);
        registerReceiver(paused, filter);
    }

    private BroadcastReceiver resume = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playing = true;
            loadPlayer();
        }
    };

    private void registerResume() {
        IntentFilter filter = new IntentFilter(Action_Play);
        registerReceiver(resume, filter);
    }

    //Broadcast Receivers end

    //Loading and controlling media player

    public void playPause(View view) {
        if (serviceBound) {
            if (playing) {
                mediaPlayer.pauseMedia();
                pb.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                con.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                playing = false;
            } else {
                mediaPlayer.resumeMedia();
                pb.setImageResource(R.drawable.ic_pause_white_24dp);
                con.setImageResource(R.drawable.ic_pause_white_24dp);
                playing = true;
            }
        } else {
            Toast.makeText(getApplicationContext(), "Play some audio first", Toast.LENGTH_LONG).show();
        }
    }

    private void loadPlayer() {
        if (isLoading) {
            ct.setText(trackTitle);
            st.setText(trackSub);
            tt.setText(trackTitle);
            ts.setText(trackSub);
            du.setText(duration);

            Glide.with(this)
                    .asBitmap()
                    .load(trackImg)
                    .into(iv1);
            Glide.with(this)
                    .asBitmap()
                    .load(trackImg)
                    .into(iv2);

            pb.setVisibility(View.GONE);
            con.setVisibility(View.GONE);

            loading.setVisibility(View.VISIBLE);
            loadingExp.setVisibility(View.VISIBLE);
            mSeekBar.setEnabled(false);
        } else {

            ct.setText(trackTitle);
            st.setText(trackSub);
            tt.setText(trackTitle);
            ts.setText(trackSub);
            du.setText(duration);

            if (haveTrack) {
                Glide.with(this)
                        .asBitmap()
                        .load(trackImg)
                        .into(iv1);
                Glide.with(this)
                        .asBitmap()
                        .load(trackImg)
                        .into(iv2);
            } else {
                iv1.setImageResource(R.drawable.ic_launcher_background);
                iv2.setImageResource(R.drawable.ic_launcher_background);
            }

            mSeekBar.setMax(intDuration);

            loadingExp.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);

            pb.setVisibility(View.VISIBLE);
            con.setVisibility(View.VISIBLE);

            mSeekBar.setEnabled(true);

            if (playing) {
                pb.setImageResource(R.drawable.ic_pause_white_24dp);
                con.setImageResource(R.drawable.ic_pause_white_24dp);
            } else {
                pb.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                con.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            }

            if (haveTrack) {
                mSeekBar.setProgress(currentTime);
                cp.setText(currentStringTime);
                mHandler = new Handler();
                mHandler.postDelayed(mUpdateTimeTask, 100);
            }
        }
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (haveTrack) {
                running = true;
                int currentPos = mediaPlayer.getCurrentPosition();
                currentTime = currentPos;
                int seconds = (currentPos / 1000) % 60;
                long minutes = (currentPos / 1000 - seconds) / 60;

                String cPos = "";

                if (minutes < 10) {
                    cPos += "0";
                }
                cPos += Long.toString(minutes);
                cPos += ":";
                if (seconds < 10) {
                    cPos += "0";
                }
                cPos += Integer.toString(seconds);

                cp.setText(cPos);
                currentStringTime = cPos;

                mHandler.postDelayed(this, 100);
                mSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            }
        }
    };


    //Binding Service to the client

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //we're bound to local service cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) iBinder;
            mediaPlayer = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    //binding service end

    //methods to add click listeners to more buttons
    public void whatsNew(View view) {
        if (isOnline()) {
            Intent mIntent = new Intent(this, WhatsNew.class);
            startActivity(mIntent);
            overridePendingTransition(0, 0);
        }
    }

    public void allArtists(View view) {
        if (isOnline()) {
            Intent mIntent = new Intent(this, Artists.class);
            startActivity(mIntent);
            overridePendingTransition(0, 0);
        }
    }

    public void allYears(View view) {
        if (isOnline()) {
            Intent mIntent = new Intent(this, Years.class);
            startActivity(mIntent);
            overridePendingTransition(0, 0);
        }
    }

    //methods to add click listeners to more buttons end

    //Method to manage Shared preferences

    private void writeData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("title", trackTitle);
        editor.putString("sub", trackSub);
        editor.putString("img", trackImg);
        editor.putBoolean("loading", isLoading);
        editor.putBoolean("playing", playing);
        editor.putBoolean("haveTrack", haveTrack);
        editor.putInt("currentTime", currentTime);
        editor.putString("currentStringTime", currentStringTime);
        editor.putInt("intDuration", intDuration);
        editor.putString("duration", duration);
        editor.apply();
    }

    //Method to manage shared preferences end

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (expanded) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (expanded) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            if (doubleBackToExitPressedOnce) {
                clearData();
                haveTrack = false;
                if (serviceBound) {
                    mediaPlayer.stopMedia();
                    mediaPlayer.stopSelf();
                }
                if (running) {
                    mHandler.removeCallbacks(mUpdateTimeTask);
                }
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }


    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }


    private class TryAgain implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            loadActivity();
        }
    }

}