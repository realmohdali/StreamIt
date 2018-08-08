package it.stream.streamit;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

public class YearPagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private Context context;
    private List<ArtistInYearList> mList;
    private String year;
    private Album_Tab_Fragment tab;

    public YearPagerAdapter(FragmentManager fm, int mNumOfTabs, Context context, List<ArtistInYearList> mList, String year) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.context = context;
        this.mList = mList;
        this.year = year;
        this.tab = tab;
    }

    @Override
    public Fragment getItem(int i) {
        tab=new Album_Tab_Fragment();
        tab.setArtist(mList.get(i).getArtist());
        tab.setYear(year);
        tab.setContext(context);
        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
