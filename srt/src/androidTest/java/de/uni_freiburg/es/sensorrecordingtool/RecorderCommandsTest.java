package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class RecorderCommandsTest extends BroadcastingTest {

    RecorderCommands commands = new RecorderCommands();

    @Test
    public void test_getBooleanOrString() {
        Intent intent = new Intent();
        Assert.assertTrue("empty", RecorderCommands.getBooleanOrString(intent, "i", false) == false);

        intent.removeExtra("i");
        intent.putExtra("i", "true");
        Assert.assertTrue("S", RecorderCommands.getBooleanOrString(intent, "i", false) == true);
        intent.removeExtra("i");
        intent.putExtra("i", "false");
        Assert.assertTrue("S", RecorderCommands.getBooleanOrString(intent, "i", false) == false);

        intent.putExtra("i", true);
        Assert.assertTrue("S", RecorderCommands.getBooleanOrString(intent, "i", false) == true);
        intent.removeExtra("i");
        intent.putExtra("i", false);
        Assert.assertTrue("S", RecorderCommands.getBooleanOrString(intent, "i", false) == false);

    }

    @Test
    public void test_getIntFloatOrDoubleArray() {
        Intent intent = new Intent();
        Assert.assertTrue("empty", Arrays.equals(RecorderCommands.getIntFloatOrDoubleArray(intent, "i", -1d), new double[]{-1d}));
        intent.putExtra("i", new int[]{1, 2, 3});
        Assert.assertTrue("int[] -> double[]", Arrays.equals(RecorderCommands.getIntFloatOrDoubleArray(intent, "i", -1d), new double[]{1d, 2d, 3d}));
        intent.removeExtra("i");
        intent.putExtra("i", new float[]{1, 2, 3});
        Assert.assertTrue("float[] -> double[]", Arrays.equals(RecorderCommands.getIntFloatOrDoubleArray(intent, "i", -1d), new double[]{1d, 2d, 3d}));
        intent.removeExtra("i");
        intent.putExtra("i", new double[]{1, 2, 3});
        Assert.assertTrue("double[] -> double[]", Arrays.equals(RecorderCommands.getIntFloatOrDoubleArray(intent, "i", -1d), new double[]{1d, 2d, 3d}));
        intent.removeExtra("i");
        intent.putExtra("i", 1);
        Assert.assertTrue("int -> double[]", Arrays.equals(RecorderCommands.getIntFloatOrDoubleArray(intent, "i", -1d), new double[]{1d}));
        intent.removeExtra("i");
        intent.putExtra("i", 1f);
        Assert.assertTrue("float -> double[]", Arrays.equals(RecorderCommands.getIntFloatOrDoubleArray(intent, "i", -1d), new double[]{1d}));
        intent.removeExtra("i");
        intent.putExtra("i", 1d);
        Assert.assertTrue("double -> double[]", Arrays.equals(RecorderCommands.getIntFloatOrDoubleArray(intent, "i", -1d), new double[]{1d}));
    }
}
