package it.stream.streamit.database;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import it.stream.streamit.dataList.ListItem;

public class RetrieveSearchedData {
    private SQLiteDatabase database;
    private List<ListItem> mList;

    public RetrieveSearchedData(SQLiteDatabase database) {
        this.database = database;
        mList = new ArrayList<>();
    }

    public List<ListItem> getData(String s) {
        Cursor cursor = database.rawQuery("SELECT * FROM allTracks WHERE title LIKE '%" + s + "%'", null);
        while (cursor.moveToNext()) {
            String title = cursor.getString(1);
            String artist = cursor.getString(2);
            String url = cursor.getString(3);
            String image = cursor.getString(4);
            String year = cursor.getString(5);

            ListItem li = new ListItem(title, artist, image, url, year);
            mList.add(li);
        }
        cursor.close();
        return mList;
    }
}
