package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
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
                .getString(SettingsActivity.PREF_KEY_OUTPUTDIR, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()) );

        intent.putExtra(Recorder.RECORDER_INPUT, sensors); // sensors
        intent.putExtra(Recorder.RECORDER_RATE, rates); // rates
        intent.putExtra(Recorder.RECORDER_OUTPUT, new File(target, de.uni_freiburg.es.sensorrecordingtool.RecorderCommands.getDefaultFileName(context)).toString());
        intent.putExtra(Recorder.RECORDER_DURATION, sRecordingDurationSec * 1d); // duration as doubles
        context.sendBroadcast(intent);
    }


    public static void sendCancelIntent(Context context) {
        Intent intent = new Intent(Recorder.CANCEL_ACTION);
        context.sendBroadcast(intent);
    }
}
