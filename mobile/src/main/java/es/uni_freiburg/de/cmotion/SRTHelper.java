package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;

import de.uni_freiburg.es.sensorrecordingtool.*;
import de.uni_freiburg.es.sensorrecordingtool.sensors.AudioSensor;
import es.uni_freiburg.de.cmotion.model.SensorModel;
import es.uni_freiburg.de.cmotion.ui.DigitEditDialog;


public class SRTHelper {

    public static double sRecordingDurationSec = -1;
    public static DigitEditDialog.OnTextChangedListener mDurationListener = new DigitEditDialog.OnTextChangedListener() {
        @Override
        public void onTextChanged(Object tag, String newText) {
            sRecordingDurationSec = Double.parseDouble(newText);
        }
    };

    public static List<SensorModel> getAvailableSensors(Context context) {
        ArrayList<SensorModel> availableSensors = new ArrayList<>();
        // SensorManager mgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        // for (android.hardware.Sensor s : mgr.getSensorList(android.hardware.Sensor.TYPE_ALL))
        //     availableSensors.add(new SensorModel(s.getName()));

        SensorModel audio = new SensorModel("Audio", true);
        audio.setSamplingRate(AudioSensor.getAudioSampleRate()); // Audio won't work with 50ms
        availableSensors.add(audio);
        availableSensors.add(new SensorModel("Location"));
        availableSensors.add(new SensorModel("Video"));
        availableSensors.add(new SensorModel("accelerometer", true));
        availableSensors.add(new SensorModel("gyroscope", true));
        availableSensors.add(new SensorModel("mag", true));
        availableSensors.add(new SensorModel("pressure"));
        availableSensors.add(new SensorModel("Rotation Vector", true));

        Collections.sort(availableSensors); // Sort alphabetically
        return availableSensors;
    }

    public static void sendRecordIntent(Context context, List<SensorModel> selectedList) {
        Intent intent = new Intent(Recorder.RECORD_ACTION);

        String[] sensors = new String[selectedList.size()];
        int[] rates = new int[selectedList.size()];
        int i = 0;
        for(SensorModel model : selectedList) {
            sensors[i] = model.getName().toLowerCase();
            rates[i] = model.getSamplingRate();
            i++;
        }

        File target = new File( PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(SettingsActivity.PREF_KEY_OUTPUTDIR, null) );
        target = target!=null ? target :
                 Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        intent.putExtra(Recorder.RECORDER_INPUT, sensors); // sensors
        intent.putExtra(Recorder.RECORDER_RATE, rates); // rates
        intent.putExtra(Recorder.RECORDER_OUTPUT, new File(target, de.uni_freiburg.es.sensorrecordingtool.RecorderCommands.getDefaultFileName()).toString());
        intent.putExtra(Recorder.RECORDER_DURATION, sRecordingDurationSec * 1d); // duration as doubles
        context.sendBroadcast(intent);
    }


    public static void sendCancelIntent(Context context) {
        Intent intent = new Intent(Recorder.CANCEL_ACTION);
        context.sendBroadcast(intent);
    }
}
