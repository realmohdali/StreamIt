package it.stream.streamit;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

public class ArtistPagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private Context context;
    private List<YearInArtistList> mList;
    private String artist;
    private Album_Tab_Fragment tab;

    public ArtistPagerAdapter(FragmentManager fm, int mNumOfTabs, Context context, List<YearInArtistList> mList, String artist) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.context=context;
        this.mList=mList;
        this.artist=artist;
    }

    @Override
    public Fragment getItem(int i) {
        tab=new Album_Tab_Fragment();
        tab.setArtist(artist);
        tab.setYear(mList.get(i).getYear());
        tab.setContext(context);
        return tab;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
