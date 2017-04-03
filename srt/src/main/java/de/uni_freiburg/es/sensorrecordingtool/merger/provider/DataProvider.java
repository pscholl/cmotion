package de.uni_freiburg.es.sensorrecordingtool.merger.provider;

import android.content.Context;

import java.io.File;

public abstract class DataProvider {
    String nodeId;
    Context mContext;

    public DataProvider(Context context) {
        this.mContext = context;
    }

    public abstract void serve(String recordingUUID, File file);

}
