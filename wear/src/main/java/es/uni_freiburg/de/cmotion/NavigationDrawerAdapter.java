package es.uni_freiburg.de.cmotion;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import es.uni_freiburg.de.cmotion.fragments.RecordingFragment;
import es.uni_freiburg.de.cmotion.fragments.SelectPositionFragment;
import es.uni_freiburg.de.cmotion.fragments.TestFragment;

public class NavigationDrawerAdapter extends FragmentStatePagerAdapter {

    private final int NUM_PAGES = 3;

    NavigationDrawerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new RecordingFragment();
            case 1:
                return new SelectPositionFragment();
            case 2:
                return new TestFragment();
            default: // Shall never happen
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }
}