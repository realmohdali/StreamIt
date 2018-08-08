package it.stream.streamit;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.stream.streamit.MediaPlayerService.Action_Pause;
import static it.stream.streamit.MediaPlayerService.Action_Play;
import static it.stream.streamit.MediaPlayerService.Kill_Player;

public class YearInArtist extends AppCompatActivity {


    private Toolbar toolbar;
    private DrawerLayout mDrawerLayout;
    private boolean drawerOpen;
    private ViewPager mViewPagerControl;
    private TabLayout tabLayout;
    private RelativeLayout mRelativeLayout;
    private RelativeLayout mediaPlayerUI;

    private String mediaQueue;

    private String trackUrl;
    private SQLiteDatabase db;

    private static final String URL = "http://realmohdali.000webhostapp.com/streamIt/php_modules/showYearInArtist.php";
    private List<YearInArtistList> mList;

    private String artist, image;

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
    private ProgressBar mProgressBar;
    private ImageButton repeat;
    private ImageButton fav;
    private boolean isFav;
    private int loopStatus;

    private Handler mHandler;
    private boolean running = false;

    private BottomSheetBehavior sheetBehavior;

    //Loop strings
    private static final int noLoop = 1;
    private static final int loopAll = 2;
    private static final int loopOne = 3;

    private int marginInPx;


    //Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_years_in_artist);

        db = openOrCreateDatabase("favorite", MODE_PRIVATE, null);

        mediaPlayerUI = findViewById(R.id.bottom_sheet);
        mediaPlayerUI.setVisibility(View.GONE);

        float scale = getResources().getDisplayMetrics().density;
        marginInPx = (int) (50 * scale + 0.5f);

        mViewPagerControl = findViewById(R.id.pager);
        mRelativeLayout = findViewById(R.id.loadingDataHome);
        tabLayout = findViewById(R.id.tab_layout);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        setUpToolbar();
        setUpNavDrawer();
        loadActivity();
        loadBottomSheet();
    }

    @Override
    protected void onResume() {
        super.onResume();
        readData();

        if (haveTrack) {
            mediaPlayerUI.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPagerControl.getLayoutParams();
            params.setMargins(0, 0, 0, marginInPx);
            mViewPagerControl.setLayoutParams(params);
            loadPlayer();
        }
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
        unregisterReceiver(resetPlayerUI);
        writeData();
        if (running) {
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        writeData();
        if (running) {
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
        super.onPause();
    }

    //Activity lifecycle End

    //Methods to load data on screen

    private void loadActivity() {

        registerPlayerPrepared();
        registerBufferingUpdate();
        registerNewAudio();
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
        mProgressBar = findViewById(R.id.progressBar);
        repeat = findViewById(R.id.repeat);
        fav = findViewById(R.id.fav);
        isFav = false;
        loopStatus = noLoop;

        trackUrl = "";

        serviceBound = false;

        mList = new ArrayList<>();

        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPagerControl.getLayoutParams();
        params.setMargins(0, 0, 0, 0);
        mViewPagerControl.setLayoutParams(params);
    }

    private void setUpToolbar() {
        Bundle bundle = getIntent().getExtras();
        artist = bundle.getString("artist");
        image = bundle.getString("image");

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(artist);

        loadData();
    }

    private void setUpTabs() {
        for (int i = 0; i < mList.size(); i++) {
            tabLayout.addTab(tabLayout.newTab().setText(mList.get(i).getYear()));
        }
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

        ImageView toolbarImage = findViewById(R.id.toolbarImage);
        String imgUrl = mList.get(0).getImage();
        Glide.with(this)
                .asBitmap()
                .load(imgUrl)
                .into(toolbarImage);

        ProgressBar progressBar = findViewById(R.id.toolbarProgress);
        progressBar.setVisibility(View.GONE);
        toolbarImage.setVisibility(View.VISIBLE);

        final ViewPager viewPager = findViewById(R.id.pager);

        final ArtistPagerAdapter pagerAdapter = new ArtistPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount(), getApplicationContext(), mList, artist);

        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void loadData() {
        tabLayout.setVisibility(View.GONE);
        mViewPagerControl.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            mRelativeLayout.setVisibility(View.GONE);
                            tabLayout.setVisibility(View.VISIBLE);
                            mViewPagerControl.setVisibility(View.VISIBLE);
                            JSONArray jsonArray = new JSONArray(response);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String year = (String) jsonObject.get("year");

                                YearInArtistList li = new YearInArtistList(year, image);
                                mList.add(li);
                            }
                            setUpTabs();
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

    private void setUpNavDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        NavigationView navigationView = findViewById(R.id.nav_view);
        drawerOpen = false;
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    case R.id.homePage:
                        mDrawerLayout.closeDrawers();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.favOption:
                        mDrawerLayout.closeDrawers();
                        Intent intent1 = new Intent(getApplicationContext(), Favorite.class);
                        startActivity(intent1);
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.about:
                        mDrawerLayout.closeDrawers();
                        Toast.makeText(getApplicationContext(), "About is clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                        mDrawerLayout.closeDrawers();
                        return true;
                }
            }
        });

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {

            }

            @Override
            public void onDrawerOpened(@NonNull View view) {
                drawerOpen = true;
            }

            @Override
            public void onDrawerClosed(@NonNull View view) {
                drawerOpen = false;
            }

            @Override
            public void onDrawerStateChanged(int i) {

            }
        });
    }

    private void loadBottomSheet() {

        pb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPause(view);
            }
        });

        con.setOnClickListener(new View.OnClickListener() {
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
                if (haveTrack) {
                    if (isFav) {
                        FavoriteManagement favoriteManagement = new FavoriteManagement(trackTitle, trackUrl, trackImg, trackSub, db, getApplicationContext());
                        switch (favoriteManagement.removeFav()) {
                            case FavoriteManagement.SUCCESS:
                                fav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                                isFav = false;
                                Toast.makeText(getApplicationContext(), "Removed from favorite", Toast.LENGTH_SHORT).show();
                                break;
                            case FavoriteManagement.ALREADY_EXISTS_OR_REMOVED:
                                fav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                                isFav = false;
                                Toast.makeText(getApplicationContext(), "This track does not exist in favorite", Toast.LENGTH_SHORT).show();
                                break;
                            case FavoriteManagement.ERROR:
                                Toast.makeText(getApplicationContext(), "Error in removing from favorite", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    } else {
                        FavoriteManagement favoriteManagement = new FavoriteManagement(trackTitle, trackUrl, trackImg, trackSub, db, getApplicationContext());
                        switch (favoriteManagement.addFav()) {
                            case FavoriteManagement.SUCCESS:
                                fav.setImageResource(R.drawable.ic_favorite_green_24dp);
                                isFav = true;
                                Toast.makeText(getApplicationContext(), "Added to favorite", Toast.LENGTH_SHORT).show();
                                break;
                            case FavoriteManagement.ALREADY_EXISTS_OR_REMOVED:
                                fav.setImageResource(R.drawable.ic_favorite_green_24dp);
                                isFav = true;
                                Toast.makeText(getApplicationContext(), "This track is already exist in favorite", Toast.LENGTH_SHORT).show();
                                break;
                            case FavoriteManagement.ERROR:
                                Toast.makeText(getApplicationContext(), "Error in adding to favorite", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                }
            }
        });

        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        expanded = false;

        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                switch (i) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Toolbar playerToolbar = findViewById(R.id.player_toolbar);
                        setSupportActionBar(playerToolbar);
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        getSupportActionBar().setDisplayShowHomeEnabled(true);
                        getSupportActionBar().setTitle(R.string.playerTitle);
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
                        //playerToolbar.inflateMenu(R.menu.player_options_menu);
                        expanded = true;
                        findViewById(R.id.btmSheet).setVisibility(View.GONE);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        setSupportActionBar(toolbar);
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        getSupportActionBar().setDisplayShowHomeEnabled(false);
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
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
            public void onSlide(@NonNull View view, float v) {
                findViewById(R.id.btmSheet).setVisibility(View.VISIBLE);
                float x = 1 - v;
                findViewById(R.id.btmSheet).setAlpha(x);
            }
        });

        findViewById(R.id.btmSheet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    //Methods to load data on screen End


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
            trackUrl = intent.getExtras().getString("url");
            isFav = intent.getExtras().getBoolean("fav");
            mediaQueue = intent.getExtras().getString("playlist");

            isLoading = true;
            haveTrack = true;

            if (!serviceBound) {
                Intent bound = new Intent(getApplicationContext(), MediaPlayerService.class);
                bindService(bound, serviceConnection, Context.BIND_ABOVE_CLIENT);
            }

            mediaPlayerUI.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPagerControl.getLayoutParams();
            params.setMargins(0, 0, 0, marginInPx);
            mViewPagerControl.setLayoutParams(params);

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
            serviceBound = false;

            writeData();
            loadPlayer();

            if (expanded) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            if (running) {
                mHandler.removeCallbacks(mUpdateTimeTask);
                mediaPlayer.stopSelf();
            }

            mediaPlayerUI.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPagerControl.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            mViewPagerControl.setLayoutParams(params);
        }
    };

    private void registerResetPlayerUI() {
        IntentFilter filter = new IntentFilter(Kill_Player);
        registerReceiver(resetPlayerUI, filter);
    }

    //Broadcast Receivers end

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

    //Loading and controlling media player

    public void playPause(View view) {
        if (serviceBound) {
            if (playing) {
                mediaPlayer.pauseMedia();
                pb.setImageResource(R.drawable.ic_play_arrow);
                con.setImageResource(R.drawable.ic_play_arrow);
                playing = false;
            } else {
                mediaPlayer.resumeMedia();
                pb.setImageResource(R.drawable.ic_pause);
                con.setImageResource(R.drawable.ic_pause);
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

            if (isFav) {
                fav.setImageResource(R.drawable.ic_favorite_green_24dp);
            } else {
                fav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
            }
        } else {
            if (isFav) {
                fav.setImageResource(R.drawable.ic_favorite_green_24dp);
            } else {
                fav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
            }
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

                RelativeLayout relativeLayout = findViewById(R.id.loadingPlaylist);
                relativeLayout.setVisibility(View.VISIBLE);

                //Playlist
                Type type = new TypeToken<List<ListItem>>() {
                }.getType();
                Gson gson = new Gson();
                List<ListItem> mPlaylist = gson.fromJson(mediaQueue, type);

                RecyclerView queue = findViewById(R.id.queue);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
                queue.setLayoutManager(linearLayoutManager);

                RecyclerView.Adapter adapter = new AlbumAdapter(getApplicationContext(), mPlaylist);
                queue.setAdapter(adapter);

                relativeLayout.setVisibility(View.GONE);

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
                pb.setImageResource(R.drawable.ic_pause);
                con.setImageResource(R.drawable.ic_pause);
            } else {
                pb.setImageResource(R.drawable.ic_play_arrow);
                con.setImageResource(R.drawable.ic_play_arrow);
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
                mProgressBar.setProgress(mediaPlayer.getCurrentPosition());
            }
        }
    };

    //Loading and controlling media player Done

    //Data Related operations

    private void readData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
        loopStatus = preferences.getInt("loop", 1);
        mediaQueue = preferences.getString("playlist", "");
        isFav = preferences.getBoolean("fav", false);
    }

    private void writeData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
        editor.putBoolean("fav", isFav);
        editor.putString("playlist", mediaQueue);
        editor.putBoolean("bound", serviceBound);
        editor.apply();
    }

    //Data Related operations Done

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            mDrawerLayout.closeDrawers();
        } else if (expanded) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            writeData();
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            case R.id.srchBtn:
                Intent intent = new Intent(this, Search.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            case R.id.showQueue:
                mDrawerLayout.openDrawer(GravityCompat.END);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (expanded) {
            getMenuInflater().inflate(R.menu.player_options_menu, menu);
            return true;
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        }
    }
}
