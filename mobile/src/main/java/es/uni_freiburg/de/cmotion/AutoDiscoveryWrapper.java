package es.uni_freiburg.de.cmotion;


import android.content.Context;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.OnNodeSensorsDiscoveredListener;
import de.uni_freiburg.es.sensorrecordingtool.sensors.AudioSensor;
import es.uni_freiburg.de.cmotion.adapter.SensorAdapter;
import es.uni_freiburg.de.cmotion.model.SensorModel;

public class AutoDiscoveryWrapper {

    private final SensorAdapter mAdapter;
    private final Context mContext;
    private AutoDiscovery mAutoDiscovery;

    private OnNodeSensorsDiscoveredListener mOnNodeSensorsDiscoveredListener = new OnNodeSensorsDiscoveredListener() {
        @Override
        public void onNodeSensorsDiscovered(Node node, String[] availableSensors) {
            mAdapter.setData(convert(mAutoDiscovery.getDiscoveredSensors()));
        }
    };

    public AutoDiscoveryWrapper(Context context, SensorAdapter adapter) {

        mAdapter = adapter;
        mContext = context;
        mAutoDiscovery = AutoDiscovery.getInstance(context);
        mAutoDiscovery.setListener(mOnNodeSensorsDiscoveredListener);
//        mAutoDiscovery.discover();
    }

    private List<SensorModel> convert(ArrayList<Node> list) {

        Set<String> persistedSensors = PreferenceManager.getDefaultSharedPreferences(mContext).getStringSet("checked", new HashSet<String>());
        HashMap<String, SensorModel> sensorMap = new HashMap<>();

        for (Node key : list) {
            for (String value : key.getAvailableSensors()) {
                SensorModel sensorModel = new SensorModel(value);
                sensorModel.addAvailablePlatform(key.getPlatform());

                if (value.toLowerCase().contains("audio")) // modify predefined sample rate for audio
                    sensorModel.setSamplingRate(AudioSensor.getAudioSampleRate());

                if (value.toLowerCase().contains("video")) // TODO
                    sensorModel.setSamplingRate(15);

                if(persistedSensors.contains(sensorModel.getName()))
                    sensorModel.setEnabled(true);

                if (!sensorMap.containsKey(sensorModel.getName()))
                    sensorMap.put(sensorModel.getName(), sensorModel);
                else // we already have a sensor like this
                    sensorMap.get(sensorModel.getName()).addAvailablePlatform(key.getPlatform());
            }
        }

        List<SensorModel> ret = new ArrayList<>();
        for (String key : sensorMap.keySet()) { // add all sensors to list
            ret.add(sensorMap.get(key));
        }

        Collections.sort(ret);

        return ret;
    }

    public void close() {
        mAutoDiscovery.close();
    }

    public void refresh() {
        mAutoDiscovery.discover();
    }
}
