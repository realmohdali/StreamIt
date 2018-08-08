package it.stream.streamit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Album_Tab_Fragment extends Fragment {

    private String year, artist;
    private Context context;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private List<ListItem> mList;
    private Button playAll;

    private RelativeLayout mRelativeLayout;

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

    private void loadData() {
        mRecyclerView.setVisibility(View.GONE);
        playAll.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);

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
                                /*String link = "http://realmohdali.000webhostapp.com/streamIt/";
                                link += jsonObject.getString("url");*/
                                String link = jsonObject.getString("url");
                                String title = jsonObject.getString("title");

                                ListItem li = new ListItem(title, artist, image, link, year);
                                mList.add(li);
                            }

                            mAdapter = new AlbumAdapter(context, mList);
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
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) iBinder;
            //MediaPlayerService mediaPlayer = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void playAll() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean serviceBound = sp.getBoolean("bound", false);

        String url, title, sub, img, json;
        int size, i;

        url = mList.get(0).getURL();
        title = mList.get(0).getTitle();
        sub = mList.get(0).getArtist();
        sub += " | ";
        sub += mList.get(0).getYear();
        img = mList.get(0).getImageUrl();

        Gson gson = new Gson();
        json = gson.toJson(mList);
        size = mList.size();

        i = 0;

        if (!serviceBound) {
            Intent intent = new Intent(context, MediaPlayerService.class);
            intent.putExtra("media", url);
            intent.putExtra("title", title);
            intent.putExtra("sub", sub);
            intent.putExtra("img", img);
            intent.putExtra("playlist", json);
            intent.putExtra("size", size);
            intent.putExtra("position", i);
            context.startService(intent);
            context.bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
        } else {
            Intent intent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            intent.putExtra("media", url);
            intent.putExtra("title", title);
            intent.putExtra("sub", sub);
            intent.putExtra("img", img);
            intent.putExtra("playlist", json);
            intent.putExtra("size", size);
            intent.putExtra("position", i);
            context.sendBroadcast(intent);
        }
    }
}
