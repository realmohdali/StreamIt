package it.stream.streamit.tabFragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
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
import it.stream.streamit.adapters.ArtistHomeAdapter;
import it.stream.streamit.adapters.RecentHomeAdapter;
import it.stream.streamit.adapters.YearHomeAdapter;
import it.stream.streamit.dataList.ArtistList;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.dataList.YearList;

public class Home_Tab_Fragment extends Fragment {

    private int position;
    private Activity mActivity;
    private TextView textView;
    private Context context;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    private RelativeLayout mRelativeLayout;

    private List<ListItem> mListItems;
    private List<ArtistList> mArtistList;
    private List<YearList> mYearList;

    //URLs
    private static final String URL1 = "http://realmohdali.000webhostapp.com/streamIt/php_modules/whatsNew.php";
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
        mRecyclerView = view.findViewById(R.id.homeRecyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        mListItems = new ArrayList<>();
        mArtistList = new ArrayList<>();
        mYearList = new ArrayList<>();
        switch (position) {
            case 0:
                loadNew();
                break;
            case 1:
                loadArtist();
                break;
            case 2:
                loadYear();
                break;
        }
        return view;
    }


    private void loadNew() {
        mRecyclerView.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);
        new getNew().execute();
    }

    private void loadArtist() {
        mRecyclerView.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);
        new getArtist().execute();
    }

    private void loadYear() {
        mRecyclerView.setVisibility(View.GONE);
        mRelativeLayout.setVisibility(View.VISIBLE);
        new getYear().execute();
    }

    private class getNew extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            StringRequest mStringRequest = new StringRequest(Request.Method.GET,
                    URL1,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                mRelativeLayout.setVisibility(View.GONE);
                                mRecyclerView.setVisibility(View.VISIBLE);
                                JSONArray mJsonArray = new JSONArray(response);

                                for (int i = 0; i < mJsonArray.length(); i++) {
                                    JSONObject mJsonObject = mJsonArray.getJSONObject(i);

                                    String title = mJsonObject.getString("title");
                                    String artist = mJsonObject.getString("artist");
                                    String year = mJsonObject.getString("year");
                                    String link = mJsonObject.getString("url");
                                    String image = "http://realmohdali.000webhostapp.com/streamIt/";
                                    image += mJsonObject.getString("image");

                                    ListItem li = new ListItem(title, artist, image, link, year);
                                    mListItems.add(li);
                                }

                                mAdapter = new RecentHomeAdapter(mListItems, context);
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
            mStringRequest.setShouldCache(false);
            RequestQueue mRequest = Volley.newRequestQueue(context);
            mRequest.add(mStringRequest);

            return null;
        }
    }

    private class getArtist extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

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

                                    ArtistList li = new ArtistList(artist, image);
                                    mArtistList.add(li);
                                }
                                mAdapter = new ArtistHomeAdapter(mArtistList, context, mActivity);
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
            mStringRequest.setShouldCache(false);
            RequestQueue mRequest = Volley.newRequestQueue(context);
            mRequest.add(mStringRequest);

            return null;
        }
    }

    private class getYear extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

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

                                mAdapter = new YearHomeAdapter(context, mYearList, mActivity);
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
            mStringRequest.setShouldCache(false);
            RequestQueue mRequest = Volley.newRequestQueue(context);
            mRequest.add(mStringRequest);

            return null;
        }
    }

}
