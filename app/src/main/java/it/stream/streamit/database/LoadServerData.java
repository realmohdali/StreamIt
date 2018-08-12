package it.stream.streamit.database;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LoadServerData {
    private SQLiteDatabase database;
    private Context context;

    private static final String URL = "http://realmohdali.000webhostapp.com/streamIt/php_modules/showAll.php";
    public static final String DATA_LOADED = "it.stream.streamit.database.DATA_LOADED";

    public LoadServerData(SQLiteDatabase database, Context context) {
        this.database = database;
        this.context = context;

        database.execSQL("DROP TABLE IF EXISTS allTracks");

        database.execSQL("CREATE TABLE allTracks (_id INTEGER PRIMARY KEY, title VARCHAR, artist VARCHAR, url VARCHAR, image VARCHAR, year VARCHAR)");
    }


    public SQLiteDatabase getDatabase() {
        return database;
    }

    public Context getContext() {
        return context;
    }

    public void loadData() {
        LoadServerData data = new LoadServerData(database, context);
        new getData().execute(data);
    }

    private static class getData extends AsyncTask<LoadServerData, Void, Void> {

        @Override
        protected Void doInBackground(LoadServerData... loadServerData) {
            HttpHandler sh = new HttpHandler();

            String jsonStr = sh.makeServiceCall(URL);

            if (jsonStr != null) {
                try {
                    JSONArray jsonArray = new JSONArray(jsonStr);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        String pre = "http://realmohdali.000webhostapp.com/streamIt/";
                        String title = jsonObject.getString("title");
                        String artist = jsonObject.getString("artist");
                        String url = jsonObject.getString("url");
                        String image = pre;
                        image += jsonObject.getString("image");
                        String year = jsonObject.getString("year");

                        loadServerData[0].getDatabase().execSQL("INSERT INTO allTracks (title, artist, url, image, year) VALUES ('" + title + "','" + artist + "','" + url + "','" + image + "','" + year + "');");
                    }
                    Intent intent = new Intent(DATA_LOADED);
                    loadServerData[0].getContext().sendBroadcast(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
