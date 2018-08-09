package it.stream.streamit;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import it.stream.streamit.adapters.PagerAdapter;
import it.stream.streamit.adapters.QueueAdapter;
import it.stream.streamit.adapters.RemoveQueueItem;
import it.stream.streamit.backgroundService.MediaService;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.database.FavoriteManagement;
import it.stream.streamit.database.LoadServerData;

import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ACTION_NEXT;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ACTION_PAUSE;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ACTION_PLAY;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ACTION_PREV;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ACTION_SEEK;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ACTION_STOP;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.SEEK_UPDATE;
import static it.stream.streamit.backgroundService.MediaService.Broadcast_PLAYER_PREPARED;
import static it.stream.streamit.backgroundService.MediaService.Buffering_Update;
import static it.stream.streamit.backgroundService.MediaService.New_Audio;
import static it.stream.streamit.backgroundService.MediaService.buffering_End;

public class MainActivity extends AppCompatActivity implements RemoveQueueItem.SwipeToRemoveListener {

    private Toolbar toolbar;
    private DrawerLayout mDrawerLayout;
    private boolean drawerOpen;
    private ViewPager mViewPagerControl;
    private RelativeLayout mediaPlayerUI;
    private String trackUrl;
    private SQLiteDatabase db;


    private String mediaQueue;

    private boolean playing;
    private boolean serviceRunning;

    private Activity mActivity = (Activity) this;

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


    private BottomSheetBehavior sheetBehavior;

    //Loop strings
    private static final int noLoop = 1;
    private static final int loopAll = 2;
    private static final int loopOne = 3;

    private boolean doubleBackToExitPressedOnce = false;


    //Permission Request constants
    private static final int MY_PERMISSION_REQUEST_READ_PHONE_STATE = 1;
    private static final int MY_PERMISSION_REQUEST_ACCESS_NETWORK_STATE = 2;
    private static final int MY_PERMISSION_REQUEST_INTERNET = 3;

    private int marginInPx;

    private boolean killed = true;

    private QueueAdapter adapter;
    private int playerPosition;
    private LinearLayoutManager linearLayoutManager;

    //______________________________________________________________________________________________

    //Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        killed = sp.getBoolean("killed", true);
        if (killed) {
            clearData();
        } else {
            killed = true;
        }

        SQLiteDatabase search = openOrCreateDatabase("search", MODE_PRIVATE, null);

        if (ConnectionCheck.isConnected(this)) {
            LoadServerData loadServerData = new LoadServerData(search, this);
            loadServerData.loadData();
        }

        db = openOrCreateDatabase("favorite", MODE_PRIVATE, null);

        float scale = getResources().getDisplayMetrics().density;
        marginInPx = (int) (50 * scale + 0.5f);

        checkPermissions();

        mediaPlayerUI = findViewById(R.id.bottom_sheet);
        mediaPlayerUI.setVisibility(View.GONE);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        setUpToolbar();
        setUpTabs();
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
        unregisterReceiver(seekUpdate);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("killed", false);
        editor.apply();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        writeData();
        super.onPause();
    }

    //Activity lifecycle End
    //______________________________________________________________________________________________

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
        registerSeekUpdate();

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


        mViewPagerControl = findViewById(R.id.pager);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPagerControl.getLayoutParams();
        params.setMargins(0, 0, 0, 0);
        mViewPagerControl.setLayoutParams(params);

    }

    private void setUpToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    private void setUpTabs() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tabRecent));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tabArtist));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tabYear));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(R.id.pager);

        final PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount(), getApplicationContext(), mActivity);

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

    private void setUpNavDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        NavigationView navigationView = findViewById(R.id.nav_view);
        drawerOpen = false;
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.homePage:
                        mDrawerLayout.closeDrawers();
                        return true;
                    case R.id.favOption:
                        mDrawerLayout.closeDrawers();
                        if (ConnectionCheck.isConnected(getApplicationContext())) {
                            Intent intent = new Intent(getApplicationContext(), Favorite.class);
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.offline, Toast.LENGTH_SHORT).show();
                        }
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
                if (serviceRunning && b) {
                    Intent intent = new Intent(ACTION_SEEK);
                    intent.putExtra("pos", i);
                    sendBroadcast(intent);
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
                    Intent intent = new Intent(ACTION_PREV);
                    sendBroadcast(intent);
                }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (haveTrack) {
                    Intent intent = new Intent(ACTION_NEXT);
                    sendBroadcast(intent);
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
    //______________________________________________________________________________________________


//Broadcast Receivers

    private BroadcastReceiver playerPrepared = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            readData();
            duration = "";
            int millis = intDuration;
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

            loadPlayer();
            writeData();

            adapter.update("playing", playerPosition);
            linearLayoutManager.scrollToPosition(playerPosition);
        }
    };

    private void registerPlayerPrepared() {
        IntentFilter filter = new IntentFilter(Broadcast_PLAYER_PREPARED);
        registerReceiver(playerPrepared, filter);
    }

    private BroadcastReceiver newAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            readData();
            mediaPlayerUI.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPagerControl.getLayoutParams();
            params.setMargins(0, 0, 0, marginInPx);
            mViewPagerControl.setLayoutParams(params);

            loadPlayer();
            writeData();

            adapter.update("loading", playerPosition);
            linearLayoutManager.scrollToPosition(playerPosition);
        }
    };

    private void registerNewAudio() {
        IntentFilter filter = new IntentFilter(New_Audio);
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
        IntentFilter filter = new IntentFilter(Buffering_Update);
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
        IntentFilter filter = new IntentFilter(MediaService.Buffering);
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
        IntentFilter filter = new IntentFilter(buffering_End);
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
        IntentFilter filter = new IntentFilter(ACTION_PAUSE);
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
        IntentFilter filter = new IntentFilter(ACTION_PLAY);
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

            serviceRunning = false;

            writeData();
            loadPlayer();

            if (expanded) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            mediaPlayerUI.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPagerControl.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            mViewPagerControl.setLayoutParams(params);
        }
    };

    private void registerResetPlayerUI() {
        IntentFilter filter = new IntentFilter(ACTION_STOP);
        registerReceiver(resetPlayerUI, filter);
    }

    private BroadcastReceiver seekUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (haveTrack) {
                int currentPos = intent.getExtras().getInt("currentPosition", 0);

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

                mSeekBar.setProgress(currentPos);
                mProgressBar.setProgress(currentPos);
            }
        }
    };

    private void registerSeekUpdate() {
        IntentFilter filter = new IntentFilter(SEEK_UPDATE);
        registerReceiver(seekUpdate, filter);
    }

    //Broadcast Receivers end
    //______________________________________________________________________________________________


    //Loading and controlling media player

    public void playPause(View view) {
        if (serviceRunning) {
            if (playing) {
                Intent intent = new Intent(ACTION_PAUSE);
                sendBroadcast(intent);
                pb.setImageResource(R.drawable.ic_play_arrow);
                con.setImageResource(R.drawable.ic_play_arrow);
                playing = false;
            } else {
                Intent intent = new Intent(ACTION_PLAY);
                sendBroadcast(intent);
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

            mSeekBar.setMax(intDuration);

            loadingExp.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);

            pb.setVisibility(View.VISIBLE);
            con.setVisibility(View.VISIBLE);

            mSeekBar.setEnabled(true);

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

                mSeekBar.setProgress(currentTime);
                mProgressBar.setProgress(currentTime);
                cp.setText(currentStringTime);
            } else {
                iv1.setImageResource(R.drawable.player_background);
                iv2.setImageResource(R.drawable.player_background);
                mProgressBar.setMax(100);
                mProgressBar.setProgress(100);
            }

            if (playing) {
                pb.setImageResource(R.drawable.ic_pause);
                con.setImageResource(R.drawable.ic_pause);
            } else {
                pb.setImageResource(R.drawable.ic_play_arrow);
                con.setImageResource(R.drawable.ic_play_arrow);
            }
        }


        //Playlist
        RelativeLayout relativeLayout = findViewById(R.id.loadingPlaylist);
        relativeLayout.setVisibility(View.VISIBLE);

        Type type = new TypeToken<List<ListItem>>() {
        }.getType();
        Gson gson = new Gson();
        List<ListItem> mPlaylist = gson.fromJson(mediaQueue, type);

        RecyclerView queue = findViewById(R.id.queue);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        queue.setLayoutManager(linearLayoutManager);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RemoveQueueItem(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(queue);

        adapter = new QueueAdapter(getApplicationContext(), mPlaylist);
        queue.setAdapter(adapter);

        if (isLoading) {
            adapter.update("loading", playerPosition);
            linearLayoutManager.scrollToPosition(playerPosition);
        } else if (playing) {
            adapter.update("playing", playerPosition);
            linearLayoutManager.scrollToPosition(playerPosition);
        }

        relativeLayout.setVisibility(View.GONE);
    }

    //Loading and controlling media player Done
    //______________________________________________________________________________________________

    //Data Related operations

    private void readData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        trackTitle = preferences.getString("title", "");
        trackSub = preferences.getString("sub", "");
        trackImg = preferences.getString("img", "");
        trackUrl = preferences.getString("url", "");
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
        serviceRunning = preferences.getBoolean("serviceRunning", false);
        playerPosition = preferences.getInt("playerPosition", -1);
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
        editor.putBoolean("serviceRunning", serviceRunning);
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

    //Data Related operations Done
    //______________________________________________________________________________________________

    @Override
    public void onBackPressed() {

        if (drawerOpen) {
            mDrawerLayout.closeDrawers();
        } else if (expanded) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(getApplicationContext(), "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;

                }
            }, 2000);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (expanded) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            case R.id.srchBtn:
                if (ConnectionCheck.isConnected(this)) {
                    Intent intent = new Intent(this, Search.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(this, R.string.offline, Toast.LENGTH_SHORT).show();
                }
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
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        }
    }

    //______________________________________________________________________________________________

    //Check for permission
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSION_REQUEST_READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, MY_PERMISSION_REQUEST_ACCESS_NETWORK_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSION_REQUEST_INTERNET);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_READ_PHONE_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission granted
                } else {
                    //Permission denied
                    Toast.makeText(this, "Please grant the permission or/nThe app will crash when you receive a phone call", Toast.LENGTH_LONG).show();
                    checkPermissions();
                }
                break;
            case MY_PERMISSION_REQUEST_ACCESS_NETWORK_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission granted
                } else {
                    //Permission denied
                    Toast.makeText(this, "Please grant the permission or/nThe app will crash", Toast.LENGTH_LONG).show();
                    checkPermissions();
                }
                break;
            case MY_PERMISSION_REQUEST_INTERNET:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission granted
                } else {
                    //Permission denied
                    Toast.makeText(this, "Please grant the permission or/nThe app will crash", Toast.LENGTH_LONG).show();
                    checkPermissions();
                }
                break;
            default:
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof QueueAdapter.ViewHolder) {
            if (viewHolder.getAdapterPosition() != playerPosition) {
                adapter.removeItem(viewHolder.getAdapterPosition());
            }
        }
    }

    //Check for permission end
}
