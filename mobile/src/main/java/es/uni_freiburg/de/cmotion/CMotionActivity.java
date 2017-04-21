package es.uni_freiburg.de.cmotion;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

import es.uni_freiburg.de.cmotion.adapter.SensorAdapter;
import es.uni_freiburg.de.cmotion.shared_ui.AutoDiscoveryWrapper;
import es.uni_freiburg.de.cmotion.shared_ui.CMotionBroadcastReceiver;
import es.uni_freiburg.de.cmotion.shared_ui.RecordFloatingActionButton;
import es.uni_freiburg.de.cmotion.shared_ui.RecordingIntentFilter;
import es.uni_freiburg.de.cmotion.shared_ui.SRTHelper;
import es.uni_freiburg.de.cmotion.shared_ui.TimedProgressBar;
import es.uni_freiburg.de.cmotion.shared_ui.model.SensorModel;
import es.uni_freiburg.de.cmotion.ui.DigitEditDialog;


public class CMotionActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, CMotionBroadcastReceiver.OnRecordingStateChangedListener {


    private static final IntentFilter INTENTFILTER = new RecordingIntentFilter();
    private RecyclerView mRecyclerView;
    private RecordFloatingActionButton mRecFab;
    private SensorAdapter mRecyclerViewAdapter;
    private CMotionBroadcastReceiver mReceiver;
    private AutoDiscoveryWrapper mAutoDiscovery;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    public static DigitEditDialog.OnTextChangedListener mDurationListener = new DigitEditDialog.OnTextChangedListener() {
        @Override
        public void onTextChanged(Object tag, String newText) {
            SRTHelper.sRecordingDurationSec = Double.parseDouble(newText);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmotion);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mRecFab = (RecordFloatingActionButton) findViewById(R.id.fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerViewAdapter = new SensorAdapter(this, new ArrayList<SensorModel>());
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mReceiver = new CMotionBroadcastReceiver(this, (TimedProgressBar) findViewById(R.id.progressBar), mRecFab, (CoordinatorLayout) findViewById(R.id.coordinatorLayout));
        mReceiver.setListener(this);
        mAutoDiscovery = new AutoDiscoveryWrapper(this, mRecyclerViewAdapter);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mRecyclerViewAdapter.setExternalCheckListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SRTHelper.persistCheckedSensors(CMotionActivity.this, mRecyclerViewAdapter.getSelectedItems());
            }
        });
        mAutoDiscovery.refresh();
        registerReceiver(mReceiver, INTENTFILTER);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cmotion, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_recordingtime) {
            DigitEditDialog.build(this, "Enter Recording Duration", SRTHelper.sRecordingDurationSec + "", null, mDurationListener).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStart() {
        super.onStart();
        //startService(new Intent(this, LocalSensorService.class));
        //startService(new Intent(this, WearSensorService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        stopService(new Intent(this, LocalSensorService.class));
        stopService(new Intent(this, WearSensorService.class));
        super.onPause();
    }


    public void onFabClick(View view) {
        if (mRecFab.isRecording()) {
            SRTHelper.sendCancelIntent(this);
        } else {
            mRecyclerViewAdapter.setFrozen(true);
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            mRecFab.setFreeze(true);
            SRTHelper.sendRecordIntent(this, mRecyclerViewAdapter.getSelectedItems());

        }
    }

    public void dbeStart(View view) {
        SRTHelper.sendRecordIntent(this, mRecyclerViewAdapter.getSelectedItems());
    }

    public void dbeStop(View view) {
        SRTHelper.sendCancelIntent(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mAutoDiscovery.close();
    }



    @Override
    public void onRefresh() {
        mAutoDiscovery.refresh();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 100);
    }

    @Override
    public void onStateChanged(boolean isRecording) {
        mRecyclerViewAdapter.setFrozen(isRecording);
    }
}
