package de.uni_freiburg.es.sensorrecordingtool;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.runner.RunWith;

/** Test the ability to record videos from one of the cameras connected to the Android
 * System.
 *
 * Created by phil on 2/23/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class AudioTest {
//    private Context c;
//    private Intent i;
//    private String o;
//    private static int count = 0;
//
//    @Before public void setup() {
//        c = InstrumentationRegistry.getTargetContext();
//        i = new Intent(c, Recorder.class);
//        o = RecorderCommands.getDefaultOutputPath() + Integer.toString(count++);
//        i.putExtra("-o", o);
//    }
//
//    public void delete(File f) throws FileNotFoundException {
//        if (f.isDirectory()) {
//            for (File c : f.listFiles())
//                delete(c);
//        }
//        if (!f.delete())
//            throw new FileNotFoundException("Failed to delete file: " + f);
//    }
//
//    @After public void teardown() {
//        // not every test generates a directory.
//        try { delete(new File(o));
//        } catch (FileNotFoundException e) {}
//    }
//
//
//    public void doAudioRecording() throws InterruptedException {
//        i.putExtra("-i", "audio");
//        i.putExtra("-r", 15.);
//        i.putExtra("-d", 5.0);
//        String result = callForResult(i);
//        Assert.assertNotNull("timeout before completion", result);
//        assertRecording(result, "audio", (int) (15*5 * (1920*1080*1.5)));
//    }
//
//    public void doVideoAndOtherSensor() throws InterruptedException {
//        i.putExtra("-i", "video accelerometer".split(" "));
//        i.putExtra("-r", new double[] {15., 50.});
//        i.putExtra("-d", 5.0);
//        String result = callForResult(i);
//        Assert.assertNotNull("timeout before completion", result);
//        assertRecording(result, "video", (int) (15*5 * (1920*1080*1.5)));
//    }
//
//
//    /* we assume that some models are residing on their magnetized charging gradle while plugged
//     * in. The magnetometer will not return any data in this case, which is why we have this corner-
//     * case.     */
//    private int zeroWhenOnGradle(int i) {
//        Intent intent = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
//        boolean in  = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
//                      plugged == BatteryManager.BATTERY_PLUGGED_USB;
//
//        if (in && Build.MODEL.equalsIgnoreCase("G Watch"))
//            return 0;
//
//        return i;
//    }
//
//    public void assertRecording(String f, String p, int size) {
//        File path = new File(new File(f), p);
//        Assert.assertTrue("no output file " + path.toString(), path.exists());
//        Assert.assertEquals("wrong size", size, path.length());
//    }
//
//    private String callForError(Intent i) throws InterruptedException {
//        return callForResult(i, 15000, Recorder.ERROR_ACTION, Recorder.ERROR_REASON);
//    }
//
//    private String callForResult(Intent i) throws InterruptedException {
//        return callForResult(i, 15000, Recorder.FINISH_ACTION, Recorder.FINISH_PATH);
//    }
//
//    private String callForResult(Intent i, int ms, String action, final String extra)
//            throws InterruptedException {
//        final CountDownLatch lock = new CountDownLatch(1);
//        final String[] result = new String[] {null};
//        c.registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                result[0] = intent.getStringExtra(extra);
//                lock.countDown();
//            }
//        }, new IntentFilter(action));
//        c.startService(i);
//        return lock.await(ms, TimeUnit.MILLISECONDS) ? result[0] : null;
//    }
}
