package it.stream.streamit.download;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.stream.streamit.dataList.ListItem;

public class DownloadManagement {
    private String file_url;
    private SQLiteDatabase database;
    private Context context;
    private String file_name;

    private String title, img, artist, year;

    private List<ListItem> list;


    public DownloadManagement(String file_url, SQLiteDatabase database, Context context, String file_name, String title, String img, String artist, String year) {
        this.file_url = file_url;
        this.database = database;
        this.context = context;
        this.file_name = file_name;
        this.title = title;
        this.img = img;
        this.artist = artist;
        this.year = year;

        list = new ArrayList<>();
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS downloads (_id INTEGER PRIMARY KEY AUTOINCREMENT, file VARCHAR, file_url VARCHAR, title VARCHAR, img VARCHAR, artist VARCHAR, year VARCHAR, size INTEGER)");
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public DownloadManagement(SQLiteDatabase database) {
        this.database = database;
        list = new ArrayList<>();
        database.execSQL("CREATE TABLE IF NOT EXISTS downloads (_id INTEGER PRIMARY KEY AUTOINCREMENT, file VARCHAR, file_url VARCHAR, title VARCHAR, img VARCHAR, artist VARCHAR, year VARCHAR, size INTEGER)");
    }

    public void initDownload() {
        Toast.makeText(context, "Starting Download, Check Notification Area", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("file_url", file_url);
        intent.putExtra("file_name", file_name);
        intent.putExtra("title", title);
        intent.putExtra("image", img);
        intent.putExtra("artist", artist);
        intent.putExtra("year", year);

        context.startService(intent);

    }

    public boolean fileExists() {
        Cursor cursor = database.rawQuery("SELECT * FROM downloads WHERE file_url = '" + file_url + "'", null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public boolean deleteFile() {
        File file = context.getFileStreamPath(file_name);
        boolean fileDeleted = false;
        try {
            fileDeleted = file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fileDeleted) {
            String absolutePath = file.getAbsolutePath();
            try {
                database.execSQL("DELETE FROM downloads WHERE file = '" + file_name + "'");
                database.execSQL("UPDATE fav SET url = '" + file_url + "' WHERE url = '" + absolutePath + "'");
                database.execSQL("UPDATE recent SET url = '" + file_url + "' WHERE url = '" + absolutePath + "'");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return fileDeleted;
    }

    public List<ListItem> showDownloads() {
        Cursor cursor = database.rawQuery("SELECT * FROM downloads ORDER BY _id DESC", null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String url = cursor.getString(2);
                String title = cursor.getString(3);
                String img = cursor.getString(4);
                String artist = cursor.getString(5);
                String year = cursor.getString(6);

                ListItem item = new ListItem(title, artist, img, url, year);
                list.add(item);
            }
            cursor.close();
        }

        return list;
    }

    public String getFileLength() {
        String x = "";
        long sum = 0;
        Cursor cursor = database.rawQuery("SELECT size FROM downloads", null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                sum += cursor.getInt(0);
            }
            cursor.close();
            long sizeInMB = sum / (1024 * 1024);
            if (sizeInMB > 1024) {
                long sizeInGB = sizeInMB / 1024;
                x = "Downloaded files are taking " + sizeInGB + " GB in your storage";
            } else {
                x = "Downloaded files are taking " + sizeInMB + " MB in your storage";
            }
        }
        return x;
    }
}
