package de.uni_freiburg.es.sensorrecordingtool.merger.provider;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.uni_freiburg.es.sensorrecordingtool.merger.MergeConst;


public class WearDataProvider extends DataProvider implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final GoogleApiClient mGoogleApiClient;
    private String TAG = WearDataProvider.class.getSimpleName();


    public WearDataProvider(Context context) {
        super(context);
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.blockingConnect();
    }


    public void serve(String recordingUUID, File file) {

        if (file == null || !file.canRead() || !file.exists() || file.isDirectory())
            throw new Error("Fileaccess error");

        try {
            FileInputStream fis = new FileInputStream(file);
            int totalParts = (int) Math.ceil(file.length() / (float) MergeConst.CHUNK_SIZE);
            for (int i = 0; i < totalParts; i++) {
                byte[] buffer = new byte[i == totalParts - 1 ? (int) (file.length() % MergeConst.CHUNK_SIZE) : MergeConst.CHUNK_SIZE];
                fis.read(buffer);
                serveChunk(generateWearUri(recordingUUID), buffer, i, totalParts);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            destroy();
        }

    }

    public void destroy() {
        mGoogleApiClient.disconnect();
    }

    private String generateWearUri(String recordingUUID) {
        return MergeConst.buildWearPath(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID), recordingUUID);
    }


    /**
     * Serves a file chunk, as Wears Data API is restricted to 100kB.
     *
     * @param path
     * @param chunk
     * @param offset
     * @param total
     */
    public void serveChunk(String path, byte[] chunk, int offset, int total) {

        Log.i(TAG, "serving chunk " + offset + " to " + path);

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        putDataMapReq.getDataMap().putByteArray(MergeConst.KEY_DATA, chunk);
        putDataMapReq.getDataMap().putInt(MergeConst.KEY_OFFSET, offset);
        putDataMapReq.getDataMap().putInt(MergeConst.KEY_TOTAL, total);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.await();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
