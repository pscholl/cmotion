package de.uni_freiburg.es.sensorrecordingtool;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * A Broadcast which distributes a recording intent to the proper Services and also makes
 * sure to stop an ongoing recording if there is any.
 * <p>
 * Created by phil on 2/29/16.
 */
public class RecorderCommands extends android.content.BroadcastReceiver {

    private static final String TAG = RecorderCommands.class.getSimpleName();


    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null)
            return;

        if (Recorder.RECORD_ACTION.equals(intent.getAction()) &&
            PermissionDialog.needToAskForPermission(context)) {
            intent.setClass(context, PermissionDialog.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            /* PermissionDialog will re-broadcast the original intent, if
             * necessary permissions are granted. So we will end up here
             * again if the user pushed the right button.
             */
        } else if (Recorder.READY_ACTION.equals(intent.getAction()) && Recorder.isMaster) {
            receivedReady(intent);
        } else if (Recorder.STEADY_ACTION.equals(intent.getAction()) && !Recorder.isMaster && Recorder.isReady) {
            Recorder.mRecordUUID = intent.getStringExtra(RecorderStatus.RECORDING_UUID);
            receivedSteady(intent);
        } else if (Recorder.RECORD_ACTION.equals(intent.getAction())) {
            receivedRecord(context, intent);
        } else if (Recorder.CANCEL_ACTION.equals(intent.getAction())) {
            Recorder.stopCurrentRecording();
        }
    }

    private void receivedRecord(final Context context, final Intent intent) {
        Recorder.stopCurrentRecording();
        parseIntentOrFail(context, intent);
    }

    private void receivedSteady(Intent intent) {
        long startTime = (long) intent.getDoubleExtra(RecorderStatus.START_TIME, -1);
        Log.e(TAG, "Steady, starting recording at " + startTime);

        long correctTime = System.currentTimeMillis() + Recorder.OFFSET;
        long diff = startTime - correctTime; // Due to clock drift
        Log.e(TAG, "Waiting for " + diff + "ms");

        if (diff > 0)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Recorder.SEMAPHORE.countDown();
                }
            }, diff);
        else // time was either not set or connection took longer then the wait period was // TODO add error?
            Recorder.SEMAPHORE.countDown();
    }

    private void receivedReady(Intent intent) {

        Recorder.mReadyNodes.add(intent.getStringExtra(RecorderStatus.ANDROID_ID));

        Log.e(TAG, String.format("node %s[%s] is ready with OFFSET=%s, Semaphore at %d",
                intent.getStringExtra(RecorderStatus.ANDROID_ID),
                intent.getStringExtra(RecorderStatus.PLATFORM),
                intent.getDoubleExtra(RecorderStatus.DRIFT, 0) + " ms",
                Recorder.SEMAPHORE.getCount()));
        Recorder.SEMAPHORE.countDown();
    }

    private void parseIntentOrFail(Context context, Intent intent) {

        try {
            Intent call = parseRecorderIntent(context, intent);
            if (intent.hasExtra("forwarded"))
                call.putExtra("forwarded", true);
            call.setClass(context, Recorder.class);
            context.startService(call);
        } catch (Exception e) {
            e.printStackTrace();
            Intent i = new Intent(RecorderStatus.ERROR_ACTION);
            i.putExtra(RecorderStatus.ANDROID_ID, Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID));
            i.putExtra(RecorderStatus.ERROR_REASON, e.getMessage());
            context.sendBroadcast(i);
        }
    }

    /**
     * parse a human-generated intent into one that the recording process requires. This means
     * that some options of the Recorder can take a single value or a list of values. This function
     * will expand them accordingly and throw an Exception if the input is not usable.
     *
     * @param intent the original intent
     * @return a new machinable intent
     */
    public static Intent parseRecorderIntent(Context context, Intent intent) throws Exception {
        String output = intent.getStringExtra(Recorder.RECORDER_OUTPUT);
        String[] sensors = getStringOrArray(intent, Recorder.RECORDER_INPUT);
        double[] rates = getIntFloatOrDoubleArray(intent, Recorder.RECORDER_RATE, 50);
        String[] formats = getStringOrArray(intent, Recorder.RECORDER_FORMAT);
        double duration = getDoubleOrFloat(intent, Recorder.RECORDER_DURATION, -1.f);
        Intent call = new Intent();

        call.setAction(intent.getAction());

        output = output == null ? getDefaultOutputPath(context) : output;

        if (sensors.length <= 0)
            throw new Exception("no input supplied");

        if (formats == null)
            formats = new String[sensors.length];
        if (formats.length != sensors.length) {
            String[] fmts = new String[sensors.length];
            Arrays.fill(fmts, null);
            for (int j = 0; j < formats.length; j++)
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

    public static double getDoubleOrFloat(Intent i, String key, float v) {
        if(!i.hasExtra(key))
            return v;
        double omg = i.getExtras().get(key) instanceof  Double? i.getDoubleExtra(key, v) : v;
        return omg == v ? i.getFloatExtra(key, v) : omg;
    }

    public static boolean getBooleanOrString(Intent i, String key, boolean b) {
        boolean retVal = b;

        if (i.hasExtra(key)) {
            if ((i.getExtras().get(key)) instanceof Boolean)
                retVal = i.getBooleanExtra(key, b);
            else if ((i.getExtras().get(key)) instanceof String)
                retVal = Boolean.parseBoolean(i.getStringExtra(key));
        }

        return retVal;
    }

    public static String[] getStringOrArray(Intent i, String extra) {
        String[] arr = i.getStringArrayExtra(extra);
        if (arr != null)
            return arr;
        else if (i.getStringExtra(extra) != null)
            return new String[]{i.getStringExtra(extra)};
        else
            return new String[]{};
    }

    public static double[] getIntFloatOrDoubleArray(Intent i, String extra, double def) {
        int iarr[] = null;
        float farr[] = null;
        double darr[] = null;

        try { iarr = i.getIntArrayExtra(extra); } catch (Exception e){};
        try { farr = i.getFloatArrayExtra(extra); } catch (Exception e){};
        try { darr = i.getDoubleArrayExtra(extra); } catch (Exception e){};
        if (darr != null)
            return darr;

        if (farr != null) {
            double out[] = new double[farr.length];
            for (int j = 0; j < out.length; j++)
                out[j] = farr[j];
            return out;
        }

        if (iarr != null) {
            double out[] = new double[iarr.length];
            for (int j = 0; j < out.length; j++)
                out[j] = iarr[j];
            return out;
        }

        return new double[]{getIntFloatOrDouble(i, extra, def)};
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


    /**
     * utility function for ISO datetime path on public storage
     */
    public static String getDefaultOutputPath(Context context) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        return new File(path, getDefaultFileName(context)).toString();
    }

    public static String getDefaultFileName(Context context) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String aid = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return df.format(new Date()) + "_" + aid + ".mkv";
    }
}
