package it.stream.streamit.adapters;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import it.stream.streamit.tabFragments.Album_Tab_Fragment;
import it.stream.streamit.dataList.ArtistInYearList;

public class YearPagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private Context context;
    private List<ArtistInYearList> mList;
    private String year;
    private Album_Tab_Fragment tab;
    private SQLiteDatabase database;

    public YearPagerAdapter(FragmentManager fm, int mNumOfTabs, Context context, List<ArtistInYearList> mList, String year, SQLiteDatabase database) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.context = context;
        this.mList = mList;
        this.year = year;
        this.database = database;
    }

    @Override
    public Fragment getItem(int i) {
        tab = new Album_Tab_Fragment();
        tab.setArtist(mList.get(i).getArtist());
        tab.setYear(year);
        tab.setContext(context);
        tab.setDatabase(database);
        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
