package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Functional test for some of the functions in the Wearforwarder.
 *
 * Created by phil on 2/28/16.
 */
public class ForwarderTest {
    @Test public void testFromJson() throws JSONException {
        JSONObject o = new JSONObject().put("-i", "acceleration").put("-r", 1.);
        Bundle b = WearForwarder.fromJson(o);
        Assert.assertEquals("acceleration", b.get("-i"));
        Assert.assertEquals(1., b.getDouble("-r"), 1e8);
    }

    @Test public void testFromJsonArray() throws JSONException {
        String[] sarr = new String[] {"a", "b", "c"};
        double[] darr = new double[] {1.,2.,3.};
        JSONObject o = new JSONObject()
                .put("-i", JSONObject.wrap(sarr))
                .put("-r", JSONObject.wrap(darr));
        Bundle b = WearForwarder.fromJson(o);
        Assert.assertArrayEquals(sarr, b.getStringArray("-i"));
        Assert.assertArrayEquals(darr, b.getDoubleArray("-r"), 1e-8);
    }

    @Test public void end2end() throws JSONException {
        String[] sarr = new String[] {"a", "b", "c"};
        double[] darr = new double[] {1.,2.,3.};
        Bundle b = new Bundle();
        b.putStringArray("-i", sarr);
        b.putDoubleArray("-r", darr);
        b = WearForwarder.fromJson(WearForwarder.toJson(b));
        Assert.assertArrayEquals(sarr, b.getStringArray("-i"));
        Assert.assertArrayEquals(darr, b.getDoubleArray("-r"), 1e-8);
    }
}
