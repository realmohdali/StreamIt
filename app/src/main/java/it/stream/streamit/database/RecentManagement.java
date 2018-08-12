package it.stream.streamit.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import it.stream.streamit.dataList.ListItem;

public class RecentManagement {
    private SQLiteDatabase database;

    public RecentManagement(SQLiteDatabase database) {
        this.database = database;
        database.execSQL("CREATE TABLE IF NOT EXISTS recent (_id INTEGER PRIMARY KEY, title VARCHAR, artist VARCHAR, url VARCHAR, image VARCHAR, year VARCHAR)");
    }

    public void add(ListItem listItem) {
        String url = listItem.getURL();
        if (!alreadyExist(url)) {
            String title = listItem.getTitle();
            String artist = listItem.getArtist();
            String img = listItem.getImageUrl();
            String year = listItem.getYear();
            database.execSQL("INSERT INTO recent (title, artist, url, image, year) VALUES ('" + title + "','" + artist + "','" + url + "', '" + img + "', '" + year + "')");
        }
    }

    private boolean alreadyExist(String url) {
        Cursor cursor = database.rawQuery("SELECT * FROM recent WHERE url = '" + url + "'", null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public List<ListItem> showRecent() {
        List<ListItem> mList = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT * FROM recent", null);
        cursor.moveToLast();
        if (cursor.getCount() > 0) {
            int i = 0;
            do {
                if (i > 15) {
                    break;
                }
                String title, artist, url, img, year;
                title = cursor.getString(1);
                artist = cursor.getString(2);
                url = cursor.getString(3);
                img = cursor.getString(4);
                year = cursor.getString(5);

                ListItem li = new ListItem(title, artist, img, url, year);
                mList.add(li);
                i++;
            } while (cursor.moveToPrevious());

        }
        return mList;
    }
}