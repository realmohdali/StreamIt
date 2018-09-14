package it.stream.streamit.tabFragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import it.stream.streamit.R;
import it.stream.streamit.adapters.LatestAdapter;
import it.stream.streamit.adapters.RecentAdapter;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.dataList.RecentList;
import it.stream.streamit.database.RecentManagement;

import static it.stream.streamit.download.DownloadService.DOWNLOADING;

public class Home_Tab extends Fragment {

    private SQLiteDatabase database;
    private SQLiteDatabase favDatabase;
    private Activity mActivity;
    private Context context;
    private RecentAdapter mAdapter;

    private List<RecentList> list;

    private RecyclerView newRelease;
    private RecyclerView recentView;
    private TextView noRecent;
    private SwipeRefreshLayout refresh;
    private LinearLayout loadingNew;
    private LinearLayout loadingRecent;

    private static final String URL = "http://reimagintechnology.000webhostapp.com/matam/php_modules/showRecent.php";

    public void setmActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setDatabase(SQLiteDatabase database) {
        this.database = database;
    }

    public void setFavDatabase(SQLiteDatabase favDatabase) {
        this.favDatabase = favDatabase;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_tab, container, false);

        newRelease = view.findViewById(R.id.newRelease);

        recentView = view.findViewById(R.id.recentView);
        noRecent = view.findViewById(R.id.noRecent);

        loadingNew = view.findViewById(R.id.newLoading);
        loadingRecent = view.findViewById(R.id.recentLoading);

        refresh = view.findViewById(R.id.refresh);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadLatest();
            }
        });

        loadLatest();

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
            for (int i = 0; i < list.size(); i++) {
                mAdapter.notifyItemChanged(i);
            }
        }
    };

    private void registerDownloading() {
        IntentFilter filter = new IntentFilter(DOWNLOADING);
        context.registerReceiver(downloading, filter);
    }

    private void loadLatest() {

        newRelease.setVisibility(View.GONE);
        loadingNew.setVisibility(View.VISIBLE);

        list = new ArrayList<>();

        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String image = "http://reimagintechnology.000webhostapp.com/matam/";
                                image += jsonObject.getString("img");
                                String artist = jsonObject.getString("artist");
                                String year = jsonObject.getString("year");

                                RecentList li = new RecentList(artist, year, image);
                                list.add(li);
                            }
                            loadingNew.setVisibility(View.GONE);
                            newRelease.setVisibility(View.VISIBLE);

                            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                            newRelease.setLayoutManager(layoutManager);
                            newRelease.setAdapter(new LatestAdapter(context, list, mActivity));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        stringRequest.setShouldCache(false);
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);

        loadRecent();
    }

    private void loadRecent() {
        recentView.setVisibility(View.GONE);
        noRecent.setVisibility(View.GONE);
        loadingRecent.setVisibility(View.VISIBLE);

        RecentManagement showRecent = new RecentManagement(database);
        List<ListItem> mListItems = showRecent.showRecent();
        if (mListItems != null && mListItems.size() > 0) {
            noRecent.setVisibility(View.GONE);
        } else {
            noRecent.setVisibility(View.VISIBLE);
        }
        loadingRecent.setVisibility(View.GONE);
        recentView.setVisibility(View.VISIBLE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recentView.setLayoutManager(layoutManager);
        mAdapter = new RecentAdapter(context, mListItems, favDatabase);
        recentView.setAdapter(mAdapter);
        refresh.setRefreshing(false);
    }
}
