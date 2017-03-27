package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.List;
import java.util.Arrays;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import es.uni_freiburg.de.cmotion.model.SensorModel;
import es.uni_freiburg.de.cmotion.ui.DigitEditDialog;

import es.uni_freiburg.de.cmotion.permissions.PermissionHelperActivity;
import es.uni_freiburg.de.cmotion.permissions.PermissionRunnable;

public class SRTHelper {

    public static double sRecordingDurationSec = -1;
    public static DigitEditDialog.OnTextChangedListener mDurationListener = new DigitEditDialog.OnTextChangedListener() {
        @Override
        public void onTextChanged(Object tag, String newText) {
            sRecordingDurationSec = Double.parseDouble(newText);
        }
    };

    public static void sendRecordIntent(final Context context, List<SensorModel> selectedList) {
        final Intent intent = new Intent(Recorder.RECORD_ACTION);

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

        /** ask for permission here. This would make more sense in the srt
         * module, however srt needs to run down to Android-19 (to support
         * Glass). Runtime permissions can only be asked starting from sup
         * library 21+ */
        new PermissionHelperActivity().runWithPermissions(context,
            new PermissionRunnable() { public void run() {
              context.sendBroadcast(intent);
            }},
            new PermissionRunnable() { public void run() {
              Intent i = new Intent(RecorderStatus.ERROR_ACTION);
              i.putExtra(RecorderStatus.ERROR_REASON, "Following permissions not granted: " + Arrays.toString(notGrantedResults));
              context.sendBroadcast(i);
        }});
    }


    public static void sendCancelIntent(Context context) {
        Intent intent = new Intent(Recorder.CANCEL_ACTION);
        context.sendBroadcast(intent);
    }
}
