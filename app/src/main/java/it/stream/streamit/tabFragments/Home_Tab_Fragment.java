package it.stream.streamit.tabFragments;

import android.app.Activity;
import android.content.Context;
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
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import it.stream.streamit.R;
import it.stream.streamit.adapters.ArtistHomeAdapter;
import it.stream.streamit.adapters.YearHomeAdapter;
import it.stream.streamit.dataList.ArtistList;
import it.stream.streamit.dataList.YearList;

public class Home_Tab_Fragment extends Fragment {

    private int position;
    private Activity mActivity;
    private Context context;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;


    private SwipeRefreshLayout refreshLayout;

    private RelativeLayout mRelativeLayout;
    private RelativeLayout noRecent;

    private List<ArtistList> mArtistList;
    private List<YearList> mYearList;

    //URLs
    private static final String URL2 = "http://realmohdali.000webhostapp.com/streamIt/php_modules/showArtist.php";
    private static final String URL3 = "http://realmohdali.000webhostapp.com/streamIt/php_modules/showYear.php";

    public void setPosition(int position) {
        this.position = position;
    }

    public void setmActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void setContext(Context context) {
        this.context = context;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_tab_layout, container, false);
        mRelativeLayout = view.findViewById(R.id.loadingDataHome);
        noRecent = view.findViewById(R.id.noRecent);
        mRecyclerView = view.findViewById(R.id.homeRecyclerView);
        refreshLayout = view.findViewById(R.id.swipeToRefresh);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        mArtistList = new ArrayList<>();
        mYearList = new ArrayList<>();
        switch (position) {
            case 1:
                loadArtist();
                refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        mArtistList = new ArrayList<>();
                        loadArtist();
                    }
                });
                break;
            case 2:
                loadYear();
                refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        mYearList = new ArrayList<>();
                        loadYear();
                    }
                });
                break;
        }
        return view;
    }

    private void loadArtist() {
        noRecent.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);

        StringRequest mStringRequest = new StringRequest(Request.Method.GET,
                URL2,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            mRelativeLayout.setVisibility(View.GONE);
                            mRecyclerView.setVisibility(View.VISIBLE);
                            JSONArray jsonArray = new JSONArray(response);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String image = "http://realmohdali.000webhostapp.com/streamIt/";
                                image += jsonObject.getString("image");
                                String artist = jsonObject.getString("artist");
                                String nationality = jsonObject.getString("nationality");
                                String years = jsonObject.getString("years");

                                ArtistList li = new ArtistList(artist, image, nationality, years);
                                mArtistList.add(li);
                            }
                            FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(context);
                            layoutManager.setFlexDirection(FlexDirection.ROW);
                            layoutManager.setJustifyContent(JustifyContent.CENTER);
                            mRecyclerView.setLayoutManager(layoutManager);
                            mAdapter = new ArtistHomeAdapter(mArtistList, context, mActivity);
                            mRecyclerView.setAdapter(mAdapter);
                            refreshLayout.setRefreshing(false);
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
        mStringRequest.setShouldCache(false);
        RequestQueue mRequest = Volley.newRequestQueue(context);
        mRequest.add(mStringRequest);

    }

    private void loadYear() {
        noRecent.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);

        StringRequest mStringRequest = new StringRequest(Request.Method.GET,
                URL3,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            mRelativeLayout.setVisibility(View.GONE);
                            mRecyclerView.setVisibility(View.VISIBLE);
                            JSONArray jsonArray = new JSONArray(response);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String year = jsonObject.getString("year");
                                String image = "http://realmohdali.000webhostapp.com/streamIt/";
                                image += jsonObject.getString("image");

                                YearList li = new YearList(year, image);
                                mYearList.add(li);
                            }
                            FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(context);
                            layoutManager.setFlexDirection(FlexDirection.ROW);
                            layoutManager.setJustifyContent(JustifyContent.CENTER);
                            mRecyclerView.setLayoutManager(layoutManager);
                            mAdapter = new YearHomeAdapter(context, mYearList, mActivity);
                            mRecyclerView.setAdapter(mAdapter);
                            refreshLayout.setRefreshing(false);

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
        mStringRequest.setShouldCache(false);
        RequestQueue mRequest = Volley.newRequestQueue(context);
        mRequest.add(mStringRequest);
    }
}
