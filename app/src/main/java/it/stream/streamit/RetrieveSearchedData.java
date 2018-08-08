package it.stream.streamit;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class RetrieveSearchedData {
    private SQLiteDatabase database;
    private Context context;
    private List<ListItem> mList;
    private Cursor cursor;

    public RetrieveSearchedData(SQLiteDatabase database, Context context) {
        this.database = database;
        this.context = context;
        mList = new ArrayList<>();
    }

    public List<ListItem> getData(String s) {
        cursor = database.rawQuery("SELECT * FROM allTracks WHERE title LIKE '%"+s+"%'", null);
        while (cursor.moveToNext()) {
            String title = cursor.getString(1);
            String artist = cursor.getString(2);
            String url = cursor.getString(3);
            String image = cursor.getString(4);
            String year = cursor.getString(5);

            ListItem li = new ListItem(title, artist, image, url, year);
            mList.add(li);
        }
        return mList;
    }
}
