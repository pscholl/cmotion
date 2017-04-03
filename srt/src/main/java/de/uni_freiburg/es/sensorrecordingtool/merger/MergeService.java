package de.uni_freiburg.es.sensorrecordingtool.merger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;

public class MergeService extends Service {

    private static final String TAG = MergeService.class.getSimpleName();
    public static final String RELEVANT_AIDS = "relevant_aids";
    private AutoDiscovery mAutoDiscovery;
    private ArrayList<MergeSession> mSessionList = new ArrayList<>();

    public MergeService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAutoDiscovery = AutoDiscovery.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return super.onStartCommand(intent, flags, startId);

        String recordUUID = intent.getStringExtra(RecorderStatus.RECORDING_UUID);
        ArrayList<String> nodeAidList = intent.getStringArrayListExtra(RELEVANT_AIDS);

        mSessionList.add(new MergeSession(this, recordUUID, getNodesFromAutoDiscovery(nodeAidList)));

        return super.onStartCommand(intent, flags, startId);
    }

    private ArrayList<Node> getNodesFromAutoDiscovery(ArrayList<String> aids) {
        ArrayList<Node> nodes = new ArrayList<>();

        for (String aid : aids)
            for (Node n : mAutoDiscovery.getDiscoveredSensors())
                if (n.getAid().equals(aid))
                    nodes.add(n);

        return nodes;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
