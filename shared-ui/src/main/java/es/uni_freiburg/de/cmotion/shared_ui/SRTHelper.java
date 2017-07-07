package es.uni_freiburg.de.cmotion.shared_ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import es.uni_freiburg.de.cmotion.shared_ui.model.SensorModel;

public class SRTHelper {

    public static double sRecordingDurationSec = -1;


    public static void sendRecordIntent(final Context context, List<SensorModel> selectedList) {
        final Intent intent = new Intent(Recorder.RECORD_ACTION);

        String[] sensors = new String[selectedList.size()];
        float[] rates = new float[selectedList.size()];
        int i = 0;
        for (SensorModel model : selectedList) {
            sensors[i] = model.getName().toLowerCase();
            rates[i] = model.getSamplingRate();
            i++;
        }

//        File target = new File( PreferenceManager
//                .getDefaultSharedPreferences(context)
//                .getString(SettingsActivity.PREF_KEY_OUTPUTDIR, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()) );

        intent.putExtra(Recorder.RECORDER_INPUT, sensors); // sensors
        intent.putExtra(Recorder.RECORDER_RATE, rates); // rates
//        intent.putExtra(Recorder.RECORDER_OUTPUT, new File(target, de.uni_freiburg.es.sensorrecordingtool.RecorderCommands.getDefaultFileName(context)).toString());
        intent.putExtra(Recorder.RECORDER_DURATION, sRecordingDurationSec * 1d); // duration as doubles

        context.sendBroadcast(intent);

    }

    public static void persistCheckedSensors(Context context, List<SensorModel> selected) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> sensors = new HashSet<>();
        for (SensorModel model : selected)
            sensors.add(model.getName());

        pref.edit().putStringSet("checked", sensors).commit();
    }

    public static void sendCancelIntent(Context context) {
        Intent intent = new Intent(Recorder.CANCEL_ACTION);
        context.sendBroadcast(intent);
    }
}
