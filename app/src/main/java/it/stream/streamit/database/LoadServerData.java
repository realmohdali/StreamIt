package it.stream.streamit.database;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

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

import it.stream.streamit.dataList.ListItem;

public class LoadServerData {
    private SQLiteDatabase database;
    private Context context;
    private List<ListItem> mList;

    private static final String URL = "http://realmohdali.000webhostapp.com/streamIt/php_modules/showAll.php";
    public static final String DATA_LOADED = "it.stream.streamit.database.DATA_LOADED";

    public LoadServerData(SQLiteDatabase database, Context context) {
        this.database = database;
        this.context = context;
        mList = new ArrayList<>();

        database.execSQL("DROP TABLE IF EXISTS allTracks");

        database.execSQL("CREATE TABLE allTracks (_id INTEGER PRIMARY KEY, title VARCHAR, artist VARCHAR, url VARCHAR, image VARCHAR, year VARCHAR)");
    }

    public void loadData() {
        new getData().execute();
    }

    private class getData extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                    URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                                    String pre = "http://realmohdali.000webhostapp.com/streamIt/";
                                    String title = jsonObject.getString("title");
                                    String artist = jsonObject.getString("artist");
                                    String url = jsonObject.getString("url");
                                    String image = pre;
                                    image += jsonObject.getString("image");
                                    String year = jsonObject.getString("year");

                                    ListItem li = new ListItem(title, artist, image, url, year);
                                    mList.add(li);
                                }

                                for (int i = 0; i < mList.size(); i++) {
                                    String title = mList.get(i).getTitle();
                                    String artist = mList.get(i).getArtist();
                                    String url = mList.get(i).getURL();
                                    String image = mList.get(i).getImageUrl();
                                    String year = mList.get(i).getYear();

                                    database.execSQL("INSERT INTO allTracks (title, artist, url, image, year) VALUES ('" + title + "','" + artist + "','" + url + "','" + image + "','" + year + "');");
                                }

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

            stringRequest.setShouldCache(false);
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Intent intent = new Intent(DATA_LOADED);
            context.sendBroadcast(intent);
        }
    }
}
