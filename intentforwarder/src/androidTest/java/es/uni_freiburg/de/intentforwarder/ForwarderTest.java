package es.uni_freiburg.de.intentforwarder;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.uni_freiburg.es.intentforwarder.ForwardedUtils;

/**
 * Functional test for some of the functions in the Wearforwarder.
 *
 * Created by phil on 2/28/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ForwarderTest {
    @Test public void testFromJson() throws JSONException {
        JSONObject o = new JSONObject()
                .put("extras", new JSONObject()
                    .put("-i", "acceleration")
                    .put("-r", 1.))
                .put("action", "test");

        Intent b = ForwardedUtils.fromJson(o);
        Assert.assertEquals("acceleration", b.getStringExtra("-i"));
        Assert.assertEquals(1., b.getDoubleExtra("-r", 1), 1e8);
    }

    @Test public void testFromJsonArray() throws JSONException {
        String[] sarr = new String[] {"a", "b", "c"};
        double[] darr = new double[] {1.,2.,3.};
        JSONObject o = new JSONObject()
                .put("extras", new JSONObject()
                    .put("-i", JSONObject.wrap(sarr))
                    .put("-r", JSONObject.wrap(darr)))
                .put("action", "test");
        Intent b = ForwardedUtils.fromJson(o);
        Assert.assertArrayEquals(sarr, b.getStringArrayExtra("-i"));
        Assert.assertArrayEquals(darr, b.getDoubleArrayExtra("-r"), 1e-8);
    }

    @Test public void end2end() throws JSONException {
        String[] sarr = new String[] {"a", "b", "c"};
        double[] darr = new double[] {1.,2.,3.};
        Bundle b = new Bundle();
        b.putStringArray("-i", sarr);
        b.putDoubleArray("-r", darr);
        Intent i = new Intent("test");
        i.putExtras(b);
        i = ForwardedUtils.fromJson(ForwardedUtils.toJson(i));
        Assert.assertArrayEquals(sarr, i.getStringArrayExtra("-i"));
        Assert.assertArrayEquals(darr, i.getDoubleArrayExtra("-r"), 1e-8);
    }
}
