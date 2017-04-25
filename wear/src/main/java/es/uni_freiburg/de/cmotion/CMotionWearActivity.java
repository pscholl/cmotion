package es.uni_freiburg.de.cmotion;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.WindowManager;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;

public class CMotionWearActivity extends FragmentActivity {

    private static final String TAG = CMotionWearActivity.class.getName();
    private ViewPager mViewPager;
    private NavigationDrawerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmotion_wear);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new NavigationDrawerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount()-1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AutoDiscovery.getInstance(this).close();
    }
}
