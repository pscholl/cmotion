package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/** A Broadcast which distributes a recording intent to the proper Services and also makes
 * sure to stop an ongoing recording if there is any.
 *
 * Created by phil on 2/29/16.
 */
public class RecorderCommands extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        /** stop any recording per default, whether this is a cancel or start request */
        Recorder.recording = false;

        /** parse the intent and forward only if a new recording is to be started. */
        if (!Recorder.RECORD_ACTION.equals(intent.getAction()))
            return;

        try {
            Intent call = parseRecorderIntent(intent);
            intent.setClass(context, Recorder.class);
            context.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** parse a human-generated intent into one that the recording process requires. This means
     * that some options of the Recorder can take a single value or a list of values. This function
     * will expand them accordingly and throw an Exception if the input is not usable.
     *
     * @param intent the original intent
     * @return a new machinable intent
     */
    public static Intent parseRecorderIntent(Intent intent) throws Exception {
        String output = intent.getStringExtra(Recorder.RECORDER_OUTPUT);
        String[] sensors = getStringOrArray(intent, Recorder.RECORDER_INPUT);
        double[] rates = getIntFloatOrDoubleArray(intent, Recorder.RECORDER_RATE, 50.);
        String[] formats = getStringOrArray(intent, Recorder.RECORDER_FORMAT);
        double duration = intent.getDoubleExtra(Recorder.RECORDER_DURATION, -1.);
        Intent call = new Intent();

        call.setAction(intent.getAction());

        output = output==null ? getDefaultOutputPath() : output;

        if (sensors.length < 0)
            throw new Exception("no input supplied");

        if (formats == null)
            formats = new String[sensors.length];
        if (formats.length != sensors.length) {
            String[] fmts = new String[sensors.length];
            Arrays.fill(fmts, null);
            for (int j=0; j<formats.length; j++)
                fmts[j] = formats[j];
            formats = fmts;
        }

        if (rates.length == 1) {
            rates = Arrays.copyOf(rates, sensors.length);
            Arrays.fill(rates, rates[0]);
        }

        if (rates.length != sensors.length)
            throw new Exception("either rates and sensors must be of same length or a single rate must be given");

        for (double r : rates)
            if (r <= 0)
                throw new Exception("rate must be larger than zero, but was " + r);

        call.putExtra(Recorder.RECORDER_OUTPUT, output);
        call.putExtra(Recorder.RECORDER_INPUT, sensors);
        call.putExtra(Recorder.RECORDER_RATE, rates);
        call.putExtra(Recorder.RECORDER_FORMAT, formats);
        call.putExtra(Recorder.RECORDER_DURATION, duration);

        return call;
    }

    public static String[] getStringOrArray(Intent i, String extra) {
        String[] arr = i.getStringArrayExtra(extra);
        if (arr != null)
            return arr;
        else if (i.getStringExtra(extra) != null)
            return new String[] { i.getStringExtra(extra) };
        else
            return new String[] { };
    }

    public static double[] getIntFloatOrDoubleArray(Intent i, String extra, double def) {
        int iarr[] = i.getIntArrayExtra(extra);
        float farr[] = i.getFloatArrayExtra(extra);
        double darr[] = i.getDoubleArrayExtra(extra);

        if (darr != null)
            return darr;

        if (farr != null) {
            double out[] = new double[farr.length];
            for (int j=0; j<out.length; j++)
                out[j] = farr[j];
            return out;
        }

        if (iarr != null) {
            double out[] = new double[iarr.length];
            for (int j=0; j<out.length; j++)
                out[j] = iarr[j];
            return out;
        }

        return new double[]{getIntFloatOrDouble(i,extra,def)};
    }

    public static double getIntFloatOrDouble(Intent i, String extra, double def) {
        int ivalue = i.getIntExtra(extra, -1);
        float fvalue = i.getFloatExtra(extra, -1);
        double dvalue = i.getDoubleExtra(extra, -1);

        if (dvalue != -1)
            return dvalue;

        if (fvalue != -1)
            return fvalue;

        if (ivalue != -1)
            return ivalue;

        return def;
    }


    /** utility function for ISO datetime path on public storage */
    public static String getDefaultOutputPath() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        return new File(path, df.format(new Date())).toString();
    }
}
