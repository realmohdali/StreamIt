package it.stream.streamit;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static it.stream.streamit.MediaPlayerService.Action_Pause;
import static it.stream.streamit.MediaPlayerService.Action_Play;
import static it.stream.streamit.MediaPlayerService.Kill_Player;

public class YearInArtist extends AppCompatActivity {

    private static final String URL = "http://realmohdali.000webhostapp.com/streamIt/php_modules/showYearInArtist.php";
    private String artist;
    private String image;
    private List<YearInArtistList> mList;

    private MediaPlayerService mediaPlayer;
    private boolean serviceBound;
    private boolean playing;

    //BottomSheetStuff
    //private Button pb;
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
    private ProgressBar mProgressBar;
    private ImageButton repeat;
    private ImageButton fav;
    private boolean isFav;
    private int loopStatus;

    private Handler mHandler;
    private boolean running = false;

    BottomSheetBehavior sheetBehavior;

    //Loop strings
    private static final int noLoop=1;
    private static final int loopAll=2;
    private static final int loopOne=3;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    //Activity Life Cycle start

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_year_in_artist);

        loadActivity();
        loadBottomSheet();
    }

    @Override
    protected void onPause() {
        writeData();
        super.onPause();
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
        loopStatus = preferences.getInt("loop", 0);
        loadPlayer();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(playerPrepared);
        unregisterReceiver(newAudio);
        unregisterReceiver(bufferingUpdate);
        unregisterReceiver(buffering);
        unregisterReceiver(bufferingEnd);
        unregisterReceiver(paused);
        unregisterReceiver(resume);
        unregisterReceiver(resetPlayerUI);
        if (running) {
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
        writeData();
        unbindService(serviceConnection);
        super.onDestroy();
    }

    //Activity life cycle end

    //Methods to load data on screen

    private void loadActivity() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Bundle bundle = getIntent().getExtras();
        artist = bundle.getString("artist");
        image = bundle.getString("image");

        registerPlayerPrepared();
        registerNewAudio();
        registerBufferingUpdate();
        registerBuffering();
        registerBufferingEnd();
        registerPaused();
        registerResume();
        registerResetPlayerUI();

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
        mProgressBar=findViewById(R.id.progressBar);
        repeat=findViewById(R.id.repeat);
        fav=findViewById(R.id.fav);
        isFav=false;
        loopStatus = 0;

        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

        setTitle(artist);

        mRecyclerView = findViewById(R.id.yearInArtist);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        mList = new ArrayList<>();

        loadData();
    }

    private void loadBottomSheet() {
        /**Bottom Sheet Stuff**/

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

        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    if (repeat.getMinimumHeight() == noLoop) {
                        repeat.setImageResource(R.drawable.ic_repeat_green_24dp);
                        repeat.setMinimumHeight(2);
                        loopStatus = 2;
                        writeData();
                    } else if (repeat.getMinimumHeight() == loopAll) {
                        repeat.setImageResource(R.drawable.ic_repeat_one_green_24dp);
                        repeat.setMinimumHeight(3);
                        loopStatus = 3;
                        writeData();
                    } else if (repeat.getMinimumHeight() == loopOne) {
                        repeat.setImageResource(R.drawable.ic_repeat_white_24dp);
                        repeat.setMinimumHeight(1);
                        loopStatus = 1;
                        writeData();
                    }
                }
            }
        });

        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isFav){
                    fav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                    isFav=false;
                }else{
                    fav.setImageResource(R.drawable.ic_favorite_green_24dp);
                    isFav=true;
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
                        getSupportActionBar().setTitle(R.string.playerTitle);
                        expanded = true;
                        findViewById(R.id.btmSheet).setVisibility(View.GONE);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        getSupportActionBar().setTitle(artist);
                        findViewById(R.id.btmSheet).setVisibility(View.VISIBLE);
                        expanded = false;
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        findViewById(R.id.btmSheet).setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                findViewById(R.id.btmSheet).setVisibility(View.VISIBLE);
                float x = 1 - slideOffset;
                findViewById(R.id.btmSheet).setAlpha(x);

            }
        });
    }

    private void loadData() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Data...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        try {
                            JSONArray jsonArray = new JSONArray(response);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String year = (String) jsonObject.get("year");

                                YearInArtistList li = new YearInArtistList(year, image);
                                mList.add(li);
                            }

                            mAdapter = new YearInArtistAdapter(getApplicationContext(), mList, artist);
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
                }) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();
                params.put("a", artist);
                return params;
            }
        };
        stringRequest.setShouldCache(false);
        RequestQueue mRequest = Volley.newRequestQueue(getApplicationContext());
        mRequest.add(stringRequest);
    }

    //Methods to load data on screen end

    //Broadcast Receivers

    private BroadcastReceiver playerPrepared = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //String eTime = intent.getExtras().getString("eTime");
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

    private BroadcastReceiver resetPlayerUI = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playing = false;
            intDuration = 0;
            currentTime = 0;
            isLoading = false;
            trackTitle = "Track Title";
            trackSub = "Artist | year";
            trackImg = "";
            haveTrack = false;
            currentStringTime = "00:00";
            duration = "00:00";

            writeData();
            loadPlayer();
        }
    };

    private void registerResetPlayerUI() {
        IntentFilter filter = new IntentFilter(Kill_Player);
        registerReceiver(resetPlayerUI, filter);
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
            cp.setText(currentStringTime);

            if (loopStatus == noLoop) {
                repeat.setImageResource(R.drawable.ic_repeat_white_24dp);
                repeat.setMinimumHeight(1);
            } else if (loopStatus == loopAll) {
                repeat.setImageResource(R.drawable.ic_repeat_green_24dp);
                repeat.setMinimumHeight(2);
            } else if (loopStatus == loopOne) {
                repeat.setImageResource(R.drawable.ic_repeat_one_green_24dp);
                repeat.setMinimumHeight(3);
            }

            if (haveTrack) {
                Glide.with(this)
                        .asBitmap()
                        .load(trackImg)
                        .into(iv1);
                Glide.with(this)
                        .asBitmap()
                        .load(trackImg)
                        .into(iv2);
                mProgressBar.setMax(intDuration);
            } else {
                iv1.setImageResource(R.drawable.player_background);
                iv2.setImageResource(R.drawable.player_background);
                mProgressBar.setMax(100);
                mProgressBar.setProgress(100);

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
                mProgressBar.setProgress(currentTime);
                cp.setText(currentStringTime);
                mHandler = new Handler();
                mHandler.postDelayed(mUpdateTimeTask, 100);
            }
        }
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
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
            mProgressBar.setProgress(mediaPlayer.getCurrentPosition());
        }
    };

    //Binding Service to the client

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
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
        editor.putInt("loop", loopStatus);
        editor.apply();
    }

    //Method to manage shared preferences end

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (expanded) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return true;
                } else {
                    writeData();
                    finish();
                    overridePendingTransition(0, 0);
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (expanded) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            if(running){
                mHandler.removeCallbacks(mUpdateTimeTask);
            }
            writeData();
            finish();
            overridePendingTransition(0, 0);
            super.onBackPressed();
        }
    }

}
