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

    protected final GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation = new Location("empty");
    private boolean mConnected = false;

    public LocationSensor(Context c) {
        super(c, 4);
        mGoogleApiClient = new GoogleApiClient.Builder(c)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mLastLocation.setLatitude(-1);
        mLastLocation.setLongitude(-1);
    }

    @Override
    public void prepareSensor() {
        mGoogleApiClient.connect();
    }

    @Override
    public void startRecording() {
        super.startRecording();
        int min_rate = Integer.MAX_VALUE, min_delay = Integer.MAX_VALUE;

        for (ParameterizedListener pl : mListeners) {
            min_rate = Math.min(min_rate, pl.rate);
            min_delay = Math.min(min_delay, pl.delay);
        }

        LocationRequest req = new LocationRequest();
        req.setInterval(min_rate);
        req.setFastestInterval(min_rate);
        req.setMaxWaitTime(min_delay);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);
    }

    @Override
    public String getStringType() {
        return "android.hardware.sensor.Location";
    }

    @Override
    public void unregisterListener(SensorEventListener l) {
        super.unregisterListener(l);

        if (mListeners.size() == 0 && mConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            mConnected = false;
        }
    }

    @Override
    public void registerListener(SensorEventListener l, int rate, int delay, String format, Handler h) {
        super.registerListener(l, rate, delay, format, h);

        //if (!PermissionDialog.location(mContext))
        //    return;


        onNewListener();
    }

    @Override
    protected void onNewListener() {
        if (!mConnected)
            return;

        int min_rate = Integer.MAX_VALUE, min_delay = Integer.MAX_VALUE;

        for (ParameterizedListener pl : mListeners) {
            min_rate = Math.min(min_rate, pl.rate);
            min_delay = Math.min(min_delay, pl.delay);
        }

        LocationRequest req = new LocationRequest();
        req.setInterval(min_rate);
        req.setFastestInterval(min_rate);
        req.setMaxWaitTime(min_delay);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mConnected = true;
        setPrepared();
        onNewListener();
        if (l != null) onLocationChanged(l);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void flush(SensorEventListener l) {
        onLocationChanged(mLastLocation);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
            mLastLocation = location;

        mEvent.timestamp = System.currentTimeMillis() * 1000 * 1000;
        mEvent.values[0] = (float) mLastLocation.getLatitude();
        mEvent.values[1] = (float) mLastLocation.getLongitude();
        mEvent.values[2] = (float) mLastLocation.getAltitude();
        mEvent.values[3] = (float) mLastLocation.getAccuracy();

        notifyListeners();
    }
}
