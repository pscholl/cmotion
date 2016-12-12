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
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.OnNodeSensorsDiscoveredListener;
import de.uni_freiburg.es.sensorrecordingtool.sensors.AudioSensor;
import es.uni_freiburg.de.cmotion.model.SensorModel;

public class AutoDiscoveryWrapper {

    private final SensorAdapter mAdapter;
    private final Context mContext;
    private AutoDiscovery mAutoDiscovery;

    private OnNodeSensorsDiscoveredListener mOnNodeSensorsDiscoveredListener = new OnNodeSensorsDiscoveredListener() {
        @Override
        public void onNodeSensorsDiscovered(String nodeName, String[] availableSensors) {
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

    private List<SensorModel> convert(HashMap<String, List<String>> map) {

        Set<String> persistedSensors = PreferenceManager.getDefaultSharedPreferences(mContext).getStringSet("checked", new HashSet<String>());
        HashMap<String, SensorModel> sensorMap = new HashMap<>();

        for (String key : map.keySet()) {
            for (String value : map.get(key)) {

                String newName = value.contains(".") ? value.substring(value.lastIndexOf(".")+1).replace("_", " ") : value;

                SensorModel sensorModel = new SensorModel(newName);
                sensorModel.addAvailablePlatform(key);

                if (value.toLowerCase().contains("audio")) // modify predefined sample rate for audio
                    sensorModel.setSamplingRate(AudioSensor.getAudioSampleRate());

                if(persistedSensors.contains(sensorModel.getName()))
                    sensorModel.setEnabled(true);

                if (!sensorMap.containsKey(sensorModel.getName()))
                    sensorMap.put(sensorModel.getName(), sensorModel);
                else // we already have a sensor like this
                    sensorMap.get(sensorModel.getName()).addAvailablePlatform(key);
            }
        }

        List<SensorModel> list = new ArrayList<>();
        for (String key : sensorMap.keySet()) { // add all sensors to list
            list.add(sensorMap.get(key));
        }

        Collections.sort(list);

        return list;
    }

    public void close() {
        mAutoDiscovery.close();
    }

    public void refresh() {
        mAutoDiscovery.discover();
    }
}
