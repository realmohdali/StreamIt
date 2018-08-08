package it.stream.streamit;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FavoriteManagement {
    private String title, url, image, subtitle;
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
            database.execSQL("CREATE TABLE IF NOT EXISTS fav (_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, subtitle VARCHAR, image VARCHAR, url VARCHAR)");
        } catch (SQLException e) {
        }
    }

    public FavoriteManagement(Context context, SQLiteDatabase database) {
        this.context = context;
        this.database = database;
        mList = new ArrayList<>();
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS fav (_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, subtitle VARCHAR, image VARCHAR, url VARCHAR)");
        } catch (SQLException e) {
            Toast.makeText(context, "Error : " + e, Toast.LENGTH_SHORT).show();
        }
    }

    public FavoriteManagement(String title, String url, String image, String subtitle, SQLiteDatabase database, Context context) {
        this.title = title;
        this.url = url;
        this.image = image;
        this.subtitle = subtitle;
        this.database = database;
        this.context = context;

        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS fav (_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, subtitle VARCHAR, image VARCHAR, url VARCHAR)");
        } catch (SQLException e) {
            Toast.makeText(context, "Error : " + e, Toast.LENGTH_SHORT).show();
        }
    }

    public int addFav() {
        if (alreadyExists()) {
            return ALREADY_EXISTS_OR_REMOVED;
        } else {
            try {
                database.execSQL("INSERT INTO fav (title, subtitle, image, url) VALUES ('" + title + "','" + subtitle + "','" + image + "','" + url + "');");
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
                String title, subtitle, image, url;
                title = cursor.getString(1);
                subtitle = cursor.getString(2);
                image = cursor.getString(3);
                url = cursor.getString(4);

                ListItem item = new ListItem(title, subtitle, image, url, "");
                mList.add(item);
            }
        }
        return mList;
    }
}
