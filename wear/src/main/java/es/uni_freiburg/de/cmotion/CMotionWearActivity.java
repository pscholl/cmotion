package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CMotionWearActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    private static final String MESSAGE_API_PATH = "ROTATION_VECTOR_MESSAGE";
    private static final String TAG = CMotionWearActivity.class.getName();
    private ProgressBar mProgressBar;
    private TextView mLargeText;
    private GoogleApiClient mApiClient;
    private SensorManager mSensorManager;
    private String mTargetNode = null;
    private Handler mHandler;
    private TextView mMediumText;
    private long mCounter = 0;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmotion_wear);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mLargeText = (TextView) findViewById(R.id.largetext);
        mMediumText = (TextView) findViewById(R.id.medtext);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        /*
         * initialize a wearable connection
         */
        mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();

        /*
         * register a sensor listener for the local sensors
         */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME, 0);

        mHandler = new Handler();
        super.onResume();
    }

    @Override
    protected void onPause() {
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
                        if (result.getNodes().size() > 0)
                            mTargetNode = result.getNodes().get(0).getId();
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
        Wearable.MessageApi.sendMessage(mApiClient, mTargetNode, MESSAGE_API_PATH,
                ByteBuffer.allocate(4 * 4).order(ByteOrder.LITTLE_ENDIAN)
                        .putFloat(rot[0]) // q
                        .putFloat(rot[1]) // x
                        .putFloat(rot[2]) // y
                        .putFloat(rot[3]) // z
                        .array());

        mCounter++;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "CMotionWear Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://es.uni_freiburg.de.cmotion/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "CMotionWear Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://es.uni_freiburg.de.cmotion/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
