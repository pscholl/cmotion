package de.uni_freiburg.es.sensorrecordingtool.merger.retriever;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.merger.IOUtils;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeConst;

public class WearDataRetriever extends DataRetriever implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = WearDataRetriever.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;

    private int counter = 0;

    private CountDownLatch latch = new CountDownLatch(1);

    public WearDataRetriever(Context context, Node node, String recordingUUID) {
        super(context, node, recordingUUID);

        Log.i(TAG, "listening to " + MergeConst.buildWearPath(node.getAid(), mRecordingUUID));

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.blockingConnect();
    }

    @Override
    public void destroy() {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);

        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
    }


    @Override
    public File getFile() throws InterruptedException {
        latch.await();
        return getDestinationFile();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals(MergeConst.buildWearPath(mNode.getAid(), mRecordingUUID))) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    int total = dataMap.getInt(MergeConst.KEY_TOTAL);
                    int offset = dataMap.getInt(MergeConst.KEY_OFFSET);
                    byte[] data = dataMap.getByteArray(MergeConst.KEY_DATA);
                    writeChunkToDrive(data, offset, total);
                } else
                    Log.i(TAG, "skipping " + item.getUri().toString());
            }
        }
    }

    private void writeChunkToDrive(byte[] data, int offset, int total) {
        try {
            File file = getDestinationFile(offset);
            Log.i(TAG, "writing " + file.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
            counter++;
            if (counter == total) {
                destroy();
                mergeChunks();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void mergeChunks() throws IOException {
        File[] source = new File[counter];
        for (int i = 0; i < source.length; i++) {
            File chunk = getDestinationFile(i);
            source[i] = chunk;
        }
        Arrays.sort(source, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareTo(b.getName());
            }
        });

        if (getDestinationFile().exists())
            getDestinationFile().delete();
        IOUtils.joinFiles(getDestinationFile(), source);
        for (int i = 0; i < counter; i++) {
            getDestinationFile(i).delete();
        }
        latch.countDown();
    }
}
