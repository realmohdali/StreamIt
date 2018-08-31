package it.stream.streamit.database;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import it.stream.streamit.dataList.ListItem;

public class FavoriteManagement {
    private String title, url, image, artist, year;
    private Context context;
    private SQLiteDatabase database;
    private Cursor cursor;

    private List<ListItem> mList;

    public static final int SUCCESS = 1;
    public static final int ALREADY_EXISTS_OR_REMOVED = 2;
    public static final int ERROR = 3;

    public static String List_Changed = "it.stream.streamit.LIST_CHANGED";

    public FavoriteManagement(String url, SQLiteDatabase database) {
        this.url = url;
        this.database = database;
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS fav (_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, artist VARCHAR, year VARCHAR, image VARCHAR, url VARCHAR)");
        } catch (SQLException e) {
        }
    }

    public FavoriteManagement(Context context, SQLiteDatabase database) {
        this.context = context;
        this.database = database;
        mList = new ArrayList<>();
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS fav (_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, artist VARCHAR, year VARCHAR, image VARCHAR, url VARCHAR)");
        } catch (SQLException e) {
            Toast.makeText(context, "Error : " + e, Toast.LENGTH_SHORT).show();
        }
    }

    public FavoriteManagement(String title, String url, String image, String artist, String year, SQLiteDatabase database, Context context) {
        this.title = title;
        this.url = url;
        this.image = image;
        this.artist = artist;
        this.year = year;
        this.database = database;
        this.context = context;

        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS fav (_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, artist VARCHAR, year VARCHAR, image VARCHAR, url VARCHAR)");
        } catch (SQLException e) {
            Toast.makeText(context, "Error : " + e, Toast.LENGTH_SHORT).show();
        }
    }

    public int addFav() {
        if (alreadyExists()) {
            return ALREADY_EXISTS_OR_REMOVED;
        } else {
            try {
                database.execSQL("INSERT INTO fav (title, artist, year, image, url) VALUES ('" + title + "','" + artist + "','" + year + "','" + image + "','" + url + "');");
                Intent intent = new Intent(List_Changed);
                context.sendBroadcast(intent);
                return SUCCESS;
            } catch (SQLException e) {
                Toast.makeText(context, "Error : " + e, Toast.LENGTH_SHORT).show();
                return 0;
            }
        }
    }

    public int removeFav() {
        if (!alreadyExists()) {
            return ALREADY_EXISTS_OR_REMOVED;
        } else {
            try {
                cursor = database.rawQuery("SELECT * FROM fav WHERE url = '" + url + "'", null);
                if (cursor.moveToFirst()) {
                    database.execSQL("DELETE FROM fav WHERE url = '" + url + "'");
                    Intent intent = new Intent(List_Changed);
                    context.sendBroadcast(intent);
                    return SUCCESS;
                }
                return ERROR;
            } catch (SQLException e) {
                return ERROR;
            }
        }
    }

    public boolean alreadyExists() {
        cursor = database.rawQuery("SELECT * FROM fav WHERE URL = '" + url + "'", null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public List<ListItem> showFavList() {
        cursor = database.rawQuery("SELECT * FROM fav ORDER BY _id DESC", null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String title, artist, year, image, url;
                title = cursor.getString(1);
                artist = cursor.getString(2);
                year = cursor.getString(3);
                image = cursor.getString(4);
                url = cursor.getString(5);

                ListItem item = new ListItem(title, artist, image, url, year);
                mList.add(item);
            }
        }
        return mList;
    }
}
