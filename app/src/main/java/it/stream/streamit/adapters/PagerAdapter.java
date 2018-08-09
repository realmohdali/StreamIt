package it.stream.streamit.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import it.stream.streamit.tabFragments.Home_Tab_Fragment;

public class PagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private Home_Tab_Fragment tab;
    private Context context;
    private Activity mActivity;

    public PagerAdapter(FragmentManager fm, int mNumOfTabs, Context context, Activity mActivity) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.context = context;
        this.mActivity = mActivity;
    }

    @Override
    public Fragment getItem(int i) {
        tab = new Home_Tab_Fragment();
        tab.setPosition(i);
        tab.setContext(context);
        tab.setmActivity(mActivity);
        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
