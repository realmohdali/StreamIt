package it.stream.streamit.tabFragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.stream.streamit.R;
import it.stream.streamit.adapters.AlbumAdapter;
import it.stream.streamit.backgroundService.MediaService;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.download.DownloadManagement;

import static it.stream.streamit.download.DownloadService.DOWNLOADING;

public class Album_Tab_Fragment extends Fragment {

    private String year, artist;
    private Context context;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private List<ListItem> mList;
    private Button playAll;
    private SQLiteDatabase database;

    private RelativeLayout mRelativeLayout;

    String url, title, sub, img, json;

    private static final String URL = "http://realmohdali.000webhostapp.com/streamIt/php_modules/showAlbum.php";

    public static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setDatabase(SQLiteDatabase database) {
        this.database = database;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.album_tab_layout, container, false);
        mRecyclerView = view.findViewById(R.id.albumView);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRelativeLayout = view.findViewById(R.id.loadingDataHome);
        mList = new ArrayList<>();
        playAll = view.findViewById(R.id.playAll);
        playAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAll();
            }
        });
        loadData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerDownloading();
    }

    @Override
    public void onPause() {
        super.onPause();
        context.unregisterReceiver(downloading);
    }

    private BroadcastReceiver downloading = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (int i = 0; i < mList.size(); i++) {
                mAdapter.notifyItemChanged(i);
            }
        }
    };

    private void registerDownloading() {
        IntentFilter filter = new IntentFilter(DOWNLOADING);
        context.registerReceiver(downloading, filter);
    }

    private void loadData() {
        mRecyclerView.setVisibility(View.GONE);
        playAll.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);
        new getAlbum().execute();
    }

    private void playAll() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean serviceRunning = sp.getBoolean("serviceRunning", false);

        for (int i = 0; i < mList.size(); i++) {
            String file_url, image_url, file_title, file_artist, file_year;
            file_url = mList.get(i).getURL();
            image_url = mList.get(i).getImageUrl();
            file_title = mList.get(i).getTitle();
            file_artist = mList.get(i).getArtist();
            file_year = mList.get(i).getYear();

            DownloadManagement downloadManagement = new DownloadManagement(file_url, database, context, (file_title + ".mp3"), file_title, image_url, file_artist, file_year);
            final boolean exists = downloadManagement.fileExists();

            if (exists) {
                String fileName = file_title + ".mp3";
                File file = context.getFileStreamPath(fileName);
                String newUrl = file.getAbsolutePath();

                ListItem item = new ListItem(file_title, file_artist, image_url, newUrl, file_year);
                mList.set(i, item);
            }
        }

        int i;

        url = mList.get(0).getURL();
        title = mList.get(0).getTitle();
        sub = mList.get(0).getArtist();
        sub += " | ";
        sub += mList.get(0).getYear();
        img = mList.get(0).getImageUrl();

        writeData();

        Gson gson = new Gson();
        json = gson.toJson(mList);

        i = 0;

        if (!serviceRunning) {
            Intent intent = new Intent(context, MediaService.class);
            intent.putExtra("playlist", json);
            intent.putExtra("pos", i);
            intent.putExtra("isPlayList", true);
            context.startService(intent);
        } else {
            Intent intent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            intent.putExtra("playlist", json);
            intent.putExtra("pos", i);
            intent.putExtra("isPlayList", true);
            context.sendBroadcast(intent);
        }
    }

    private void writeData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("title", title);
        editor.putString("sub", sub);
        editor.putString("img", img);
        editor.putString("artist", artist);
        editor.putString("year", year);
        editor.putString("media", url);
        editor.apply();
    }

    private class getAlbum extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            StringRequest stringRequest = new StringRequest(Request.Method.POST,
                    URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                mRelativeLayout.setVisibility(View.GONE);
                                mRecyclerView.setVisibility(View.VISIBLE);
                                playAll.setVisibility(View.VISIBLE);
                                JSONArray jsonArray = new JSONArray(response);

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                                    String image = "http://realmohdali.000webhostapp.com/streamIt/";
                                    image += jsonObject.getString("image");
                                    String link = jsonObject.getString("url");
                                    String title = jsonObject.getString("title");

                                    ListItem li = new ListItem(title, artist, image, link, year);
                                    mList.add(li);
                                }

                                mAdapter = new AlbumAdapter(context, mList, database);
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
                    params.put("y", year);
                    params.put("a", artist);
                    return params;
                }
            };

            stringRequest.setShouldCache(false);
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);

            return null;
        }
    }
}
