package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * The Location API wrapped for the Sensor API.
 * <p>
 * Created by phil on 3/1/16.
 */
public class LocationSensor extends Sensor implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation = new Location("empty");
    private boolean mConnected = false;
    private Handler mHandler;
    int mUpdateRate = Integer.MAX_VALUE;
    private Runnable mOnTime = new Runnable() {
        @Override
        public void run() {
            mEvent.timestamp = System.currentTimeMillis() * 1000 * 1000;
            mEvent.values[0] = (float) mLastLocation.getLatitude();
            mEvent.values[1] = (float) mLastLocation.getLongitude();
            mEvent.values[2] = (float) mLastLocation.getAltitude();
            mEvent.values[3] = (float) mLastLocation.getAccuracy();

            notifyListeners();

            if (mConnected)
                mHandler.postDelayed(this, 1000 / mUpdateRate);
        }
    };

    public LocationSensor(Context c) {
        super(c, 4);
        mLastLocation.setLatitude(-1);
        mLastLocation.setLongitude(-1);
    }

    @Override
    public void prepareSensor(double rate, String format) {

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(LocationSensor.this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void startRecording() {
        super.startRecording();

        for (ParameterizedListener pl : mListeners) {
            mUpdateRate = Math.min(mUpdateRate, (int) pl.rate);
        }

        LocationRequest req = new LocationRequest();
        req.setInterval(mUpdateRate);
        req.setFastestInterval(mUpdateRate);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);
    }

    @Override
    public String getStringName() {
        return "Location";
    }

    @Override
    public String getStringType() {
        return "android.hardware.sensor.location";
    }

    @Override
    public void unregisterListener(SensorEventListener l) {
        super.unregisterListener(l);

        if (mListeners.size() == 0 && mConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            mConnected = false;
            mHandler = null;
        }
    }

    @Override
    public int getFifoSize() {
        return 0;
    }

    @Override
    public boolean isWakeupSensor() {
        return false;
    }

    @Override
    public void registerListener(SensorEventListener l, double rate, String format, Handler h) {
        super.registerListener(l, rate, format, h);

        if (mHandler != null && mHandler != h)
            throw new Error("cannot have multiple different handlers for LocationSensor");

        if (mHandler == null) {
            mHandler = h;
            mHandler.post(mOnTime);
        }

        onNewListener();
    }

    @Override
    protected void onNewListener() {
        if (!mConnected)
            return;

        int min_rate = Integer.MAX_VALUE;

        for (ParameterizedListener pl : mListeners) {
            min_rate = Math.min(min_rate, (int) pl.rate);
        }

        LocationRequest req = new LocationRequest();
        req.setInterval(min_rate);
        req.setFastestInterval(min_rate);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this, mHandler.getLooper());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mConnected = true;
        setPrepared();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void flush(SensorEventListener l) {
        onLocationChanged(mLastLocation);
        super.flush(l);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
            mLastLocation = location;
    }
}
