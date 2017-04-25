package de.uni_freiburg.es.sensorrecordingtool.merger.provider;

import android.content.Context;
import android.util.Log;

import java.io.File;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;

public class MergeProviderSession extends Thread {

    private static final String TAG = MergeProviderSession.class.getSimpleName();
    private Context mContext;
    private String mRecordUUID;
    private File mInputFile;

    public MergeProviderSession(Context context, String recordUUID, File inputFile) {
        mContext = context;
        mRecordUUID = recordUUID;
        mInputFile = inputFile;
        start();
    }

    @Override
    public void run() {
        super.run();
        ConnectionTechnology tech = ConnectionTechnology.pickBestConnectionTechnology(ConnectionTechnology.gatherConnectionList(mContext));
        DataProvider provider = pickProvider(tech);
        Log.i(TAG, "serving "+mRecordUUID+" via "+provider.getClass().getSimpleName());
        provider.serve(mRecordUUID, mInputFile);
    }

    private DataProvider pickProvider(ConnectionTechnology technology) {
        if (technology.getType().equals(ConnectionTechnology.Type.WEAR))
            return new WearDataProvider(mContext);
        else if (technology.getType().equals(ConnectionTechnology.Type.BT_CLASSIC))
            return new BTDataProvider(mContext);
        else if (technology.getType().equals(ConnectionTechnology.Type.TCP_OVER_WIFI))
            return new TCPProvider(mContext);
        else return null;
    }
}