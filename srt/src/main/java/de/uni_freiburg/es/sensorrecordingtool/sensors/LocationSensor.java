package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.hardware.*;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;

import de.uni_freiburg.es.sensorrecordingtool.PermissionDialog;

/**
 * The Location API wrapped for the Sensor API.
 *
 * Created by phil on 3/1/16.
 */
public class LocationSensor extends Sensor implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    protected final GoogleApiClient mGoogleApiClient;
    protected final LinkedList<ParameterizedListener> mListeners;
    protected final Context mContext;
    protected Location mLastLocation = new Location("empty");
    private boolean mConnected = false;

    public LocationSensor(Context c) {
        mGoogleApiClient = new GoogleApiClient.Builder(c)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        mContext = c;

        mListeners = new LinkedList<ParameterizedListener>();
        mLastLocation.setLatitude(-1);
        mLastLocation.setLongitude(-1);
    }

    @Override
    public String getStringType() {
        return "android.hardware.sensor.Location";
    }

    @Override
    public void registerListener(SensorEventListener l, int rate, int delay) {
        mListeners.add(new ParameterizedListener(l, rate/1000, delay/1000));

        if (!PermissionDialog.location(mContext))
            return;

        if (mListeners.size() > 0)
            mGoogleApiClient.connect();

        onNewListener();
    }

    @Override
    public void unregisterListener(SensorEventListener l) {
        for( Iterator<ParameterizedListener> it = mListeners.iterator(); it.hasNext(); ) {
            ParameterizedListener pl = it.next();
            if (pl.l.equals(l)) it.remove();
        }

        if (mListeners.size() == 0 && mConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            mConnected = false;
        }
    }

    @Override
    public void flush(SensorEventListener l) {
        notifyListeners();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location l =  LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (l!=null) mLastLocation = l;
        mConnected = true;
        notifyListeners();
        onNewListener();
    }

    private void onNewListener() {
        if (!mConnected)
            return;

        int min_rate = Integer.MAX_VALUE, min_delay=Integer.MAX_VALUE;

        for (ParameterizedListener pl : mListeners) {
            min_rate = Math.min(min_rate, pl.rate);
            min_delay = Math.min(min_delay, pl.delay);
        }

        LocationRequest req = new LocationRequest();
        req.setInterval(min_rate);
        req.setFastestInterval(min_rate);
        req.setMaxWaitTime(min_delay);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,req,this);
    }

    private void notifyListeners() {
        try {         // XXX yay for nice permissions
            Class<?> c = Class.forName(SensorEvent.class.getCanonicalName());
            Constructor<?> co = c.getDeclaredConstructors()[0];
            co.setAccessible(true);
            SensorEvent ev = (SensorEvent) co.newInstance(4);

            ev.timestamp = System.currentTimeMillis() * 1000 * 1000;
            ev.values[0] = (float) mLastLocation.getLatitude();
            ev.values[1] = (float) mLastLocation.getLongitude();
            ev.values[2] = (float) mLastLocation.getAltitude();
            ev.values[3] = (float) mLastLocation.getAccuracy();

            for (ParameterizedListener pl : mListeners)
                pl.l.onSensorChanged(ev);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
            mLastLocation = location;
    }

    private class ParameterizedListener {
        public ParameterizedListener(SensorEventListener li, int r, int d) {
            l = li;
            rate = r;
            delay = d;
        }

        SensorEventListener l;
        int rate, delay;
    }
}
