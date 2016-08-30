package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by phil on 8/26/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class FFMpegTest {
    Context c;

    @Before public void setup() {
        c = InstrumentationRegistry.getContext();
    }

    @Test public void callFFMpeg() throws IOException, InterruptedException {
        byte[] b = new byte[4096];
        FFMpegProcess p = FFMpegProcess.ffmpeg(c);
        int e = p.waitFor();
        int i = p.getErrorStream().read(b);

        Assert.assertTrue("return code is correct", e==1);
        Assert.assertTrue("ffmpeg in output", new String(b, 0,i).contains("ffmpeg"));
    }

    @Test public void callFFProbe() throws IOException, InterruptedException {
        byte[] b = new byte[4096];
        FFMpegProcess p = FFMpegProcess.ffprobe(c);
        int e = p.waitFor();
        int i = p.getErrorStream().read(b);

        Assert.assertTrue("return code is correct", e==1);
        Assert.assertTrue("ffprobe in output", new String(b, 0,i).contains("ffprobe"));
    }

    @Test public void encodeSomeNumbersIntoSingleChannel() throws IOException, InterruptedException {
        byte[] b = new byte[4096];
        FFMpegProcess p = FFMpegProcess.ffmpeg(c, "-f u8 -i tcp://localhost:%port?listen -f wav -y test.wav");

        p.getOutputStream(0).write(b);
        p.getOutputStream(0).close();

        int e = p.waitFor();
        int i = p.getErrorStream().read(b);
        System.err.println(new String(b, 0, i));
        Assert.assertTrue("command failed, check the logs", e==0);

    }
}
