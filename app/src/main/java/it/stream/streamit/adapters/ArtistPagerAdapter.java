package it.stream.streamit.adapters;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import it.stream.streamit.tabFragments.Album_Tab_Fragment;
import it.stream.streamit.dataList.YearInArtistList;

public class ArtistPagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private Context context;
    private List<YearInArtistList> mList;
    private String artist;
    private SQLiteDatabase database;

    public ArtistPagerAdapter(FragmentManager fm, int mNumOfTabs, Context context, List<YearInArtistList> mList, String artist, SQLiteDatabase database) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.context = context;
        this.mList = mList;
        this.artist = artist;
        this.database = database;
    }

    @Override
    public Fragment getItem(int i) {
        Album_Tab_Fragment tab = new Album_Tab_Fragment();
        tab.setArtist(artist);
        tab.setYear(mList.get(i).getYear());
        tab.setContext(context);
        tab.setDatabase(database);
        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
