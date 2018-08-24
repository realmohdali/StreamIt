package it.stream.streamit.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import it.stream.streamit.tabFragments.Home_Tab_Fragment;

public class PagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private Home_Tab_Fragment tab;
    private Context context;
    private Activity mActivity;
    private SQLiteDatabase database;
    private SQLiteDatabase favDatabase;

    public PagerAdapter(FragmentManager fm, int mNumOfTabs, Context context, Activity mActivity, SQLiteDatabase database, SQLiteDatabase favDatabase) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.context = context;
        this.mActivity = mActivity;
        this.database = database;
        this.favDatabase = favDatabase;
    }

    @Override
    public Fragment getItem(int i) {
        tab = new Home_Tab_Fragment();
        tab.setPosition(i);
        tab.setContext(context);
        tab.setmActivity(mActivity);
        tab.setDatabase(database);
        tab.setFavDatabase(favDatabase);
        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
