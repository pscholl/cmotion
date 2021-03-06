package es.uni_freiburg.de.cmotion.fragments;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import es.uni_freiburg.de.cmotion.R;


public class TestFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    private static final String MESSAGE_API_PATH = "ROTATION_VECTOR_MESSAGE";
    private ProgressBar mProgressBar;
    private TextView mLargeText;
    private GoogleApiClient mApiClient;
    private SensorManager mSensorManager;
    private String mTargetNode = null;
    private Handler mHandler;
    private TextView mMediumText;
    private long mCounter = 0;
    private long mStarttime;
    private int mOutstandig = 0;

    private final String TAG = TestFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_test, container, false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        mLargeText = (TextView) rootView.findViewById(R.id.largetext);
        mMediumText = (TextView) rootView.findViewById(R.id.medtext);
        mStarttime = System.currentTimeMillis();

        return rootView;
    }

    @Override
    public void onResume() {
        /*
         * initialize a wearable connection
         */
        mApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();

        /*
         * register a sensor listener for the local sensors
         */
        mSensorManager = (SensorManager) getContext().getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                20 * 1000, 0);

        mHandler = new Handler();
        super.onResume();
    }

    @Override
    public void onPause() {
        mSensorManager.unregisterListener(this);
        mApiClient.disconnect();
        super.onPause();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.NodeApi.getConnectedNodes(mApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult result) {
                        for (Node n : result.getNodes()) {
                            if (n.getDisplayName().contains("Nexus"))
                                mTargetNode = n.getId();  // XXX omg, wtf, use the capabilities API
                            System.out.println("message node " + mTargetNode + " name: " + n.getDisplayName());
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection suspended " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "connection to GoogleApi failed " + connectionResult);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!mApiClient.isConnected()) {
            mMediumText.setText("not connected");
            mLargeText.setText("");
            mProgressBar.setVisibility(View.GONE);
            return;
        }

        if (mTargetNode == null) {
            mMediumText.setText("no target");
            mLargeText.setText("");
            mProgressBar.setVisibility(View.GONE);
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mMediumText.setText("");

        if (mCounter > 1000)
            mLargeText.setText(mCounter / 1000 + "k");
        else if (mCounter > 1000 * 1000)
            mLargeText.setText(mCounter / (1000 * 1000) + "m");
        else if (mCounter > 1000 * 1000 * 1000)
            mLargeText.setText(mCounter / (1000 * 1000 * 1000) + "t");
        else
            mLargeText.setText("" + mCounter);

        float[] rot = new float[4];
        SensorManager.getQuaternionFromVector(rot, sensorEvent.values);
        if (mOutstandig < 3) {
            PendingResult<MessageApi.SendMessageResult> result;
            result = Wearable.MessageApi.sendMessage(mApiClient, mTargetNode, MESSAGE_API_PATH,
                    ByteBuffer.allocate(4 * 5).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt((int) (System.currentTimeMillis() - mStarttime))
                            .putFloat(rot[0]) // q
                            .putFloat(rot[1]) // x
                            .putFloat(rot[2]) // y
                            .putFloat(rot[3]) // z
                            .array());

            result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    mOutstandig--;
                }
            });
            mOutstandig++;
        }

        mCounter++;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
