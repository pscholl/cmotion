package de.uni_freiburg.es.sensorrecordingtool;

import java.net.Socket;
import java.net.InetSocketAddress;
import android.content.Context;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Created by phil on 8/26/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class FFMpegTest {
    Context c;
    private String filename;
    private File filepath;

    @Before public void setup() {
        c = InstrumentationRegistry.getContext();
        filename = "test.wv";
        filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        try { new File(filepath, filename).delete(); }
        catch (Exception e) {}
    }

    @Test public void callFFMpeg() throws IOException, InterruptedException {
        FFMpegProcess p = new FFMpegProcess.Builder().build(c);
        int e = p.waitFor();
        Assert.assertTrue("return code is correct", e==1);
    }

    @Test public void callFFProbe() throws IOException, InterruptedException {
        FFProbeProcess p = new FFProbeProcess.Builder().build(c);
        int e = p.waitFor();
        Assert.assertTrue("return code is okay", e==1);
    }

    @Test public void encodeSomeNumbersIntoSingleChannelWavpacked() throws Exception {
        byte b[] = new byte[4096];
        FFMpegProcess p = new FFMpegProcess.Builder()
                .addInputArgument("-f", "u8")
                .addInputArgument("-i", "tcp://localhost:%port?listen")
                .addOutputArgument("-f", "wv")
                .addOutputArgument("-y", filename)
                .build(c);

        Thread.sleep(50);

        p.getOutputStream(0).write(b);
        p.getOutputStream(0).close();

        int e = p.waitFor();

        Assert.assertTrue("ffmpeg exited cleanly", e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());

        /** now check if the streams were properly encoded */
        FFProbeProcess pp = new FFProbeProcess.Builder()
                .addInput(filename)
                .addShowOption("streams")
                .build(c);
        JSONArray streams = pp.getJSONResult().getJSONArray("streams");

        Assert.assertTrue("one stream", streams.length()==1);
        Assert.assertTrue("4096 samples", streams.getJSONObject(0).getDouble("duration_ts") == 4096);
        Assert.assertTrue("is wavpacked", streams.getJSONObject(0).getString("codec_name").equals("wavpack"));
    }

    @Test public void encodeSomeNumbersIntoSingleChannelMatroska() throws IOException, InterruptedException, JSONException {
        byte b[] = new byte[4096];
        FFMpegProcess p = new FFMpegProcess.Builder()
                .addInputArgument("-f", "u8")
                .addInputArgument("-i", "tcp://localhost:%port?listen")
                .setCodec("a", "wavpack")
                .addOutputArgument("-f", "matroska")
                .addOutputArgument("-y", filename)
                .build(c);

        Thread.sleep(250);

        p.getOutputStream(0).write(b);
        p.getOutputStream(0).close();

        int e = p.waitFor();

        Assert.assertTrue("ffmpeg exited cleanly", e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());

        /** now check if the streams were properly encoded */
        FFProbeProcess pp = new FFProbeProcess.Builder()
                .addInput(filename)
                .addShowOption("streams")
                .build(c);
        JSONArray streams = pp.getJSONResult().getJSONArray("streams");

        Assert.assertTrue("one stream", streams.length()==1);
        Assert.assertTrue("is wavpacked", streams.getJSONObject(0).getString("codec_name").equals("wavpack"));
    }

    @Test public void encodeMultipleIntoMatroska() throws Exception {
        byte[] b = new byte[4096];
        FFMpegProcess p = new FFMpegProcess.Builder()
                .addAudio("u8", 50, 1)
                .addAudio("u8", 1, 1)
                .addAudio("u8", 100, 1)
                .setCodec("a", "wavpack")
                .setOutput(filename, "matroska")
                .build(c);

        p.getOutputStream(0).write(b);
        p.getOutputStream(0).write(b);
        p.getOutputStream(0).write(b);
        p.getOutputStream(0).write(b);

        p.getOutputStream(1).write(b);

        p.getOutputStream(2).write(b);
        p.getOutputStream(2).write(b);
        p.getOutputStream(2).write(b);
        p.getOutputStream(2).write(b);
        p.getOutputStream(2).write(b);
        p.getOutputStream(2).write(b);
        p.getOutputStream(2).write(b);
        p.getOutputStream(2).write(b);

        Thread.sleep(250); p.getOutputStream(0).close();
        Thread.sleep(250); p.getOutputStream(1).close();
        Thread.sleep(250); p.getOutputStream(2).close();

        int e = p.waitFor();

        Assert.assertTrue("ffmpeg exited cleanly", e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());

        /** now check if the streams were properly encoded */
        FFProbeProcess pp = new FFProbeProcess.Builder()
                .addInput(filename)
                .addShowOption("streams")
                .build(c);
        JSONArray streams = pp.getJSONResult().getJSONArray("streams");

        Assert.assertTrue("one stream", streams.length()==3);
        Assert.assertTrue("is wavpacked", streams.getJSONObject(0).getDouble("sample_rate") == 50);
        Assert.assertTrue("is wavpacked", streams.getJSONObject(0).getString("codec_name").equals("wavpack"));
        Assert.assertTrue("is wavpacked", streams.getJSONObject(1).getDouble("sample_rate") == 1);
        Assert.assertTrue("is wavpacked", streams.getJSONObject(1).getString("codec_name").equals("wavpack"));
        Assert.assertTrue("is wavpacked", streams.getJSONObject(2).getDouble("sample_rate") == 100);
        Assert.assertTrue("is wavpacked", streams.getJSONObject(2).getString("codec_name").equals("wavpack"));
    }

    @Test public void encodeWithBuilder() throws Exception {
        byte[] b = new byte[4096];
        FFMpegProcess p = new FFMpegProcess.Builder()
            .addAudio("u8", 50, 1)
                .setStreamTag("name", "acceleration")
                .setStreamTag("location", "hip")
            .addAudio("u8", 1, 2)
                .setStreamTag("name", "gps")
                .setStreamTag("location", "hip")
            .setCodec("a", "wavpack")
            .setOutput(new File(filepath, filename).toString(), "matroska")
            .build(c);

        p.getOutputStream(0).write(b);
        p.getOutputStream(1).write(b);

        Thread.sleep(250); p.getOutputStream(0).close();
        Thread.sleep(250); p.getOutputStream(1).close();

        int e = p.waitFor();

        Assert.assertTrue("ffmpeg exited cleanly", e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());
    }

    @Test public void encodeWithBuilderVideo() throws Exception {
        byte[] b = new byte[4096],
               a = new byte[10*320*240*12/8]; // nv21 has 12bit per pixel
        new Random().nextBytes(a);

        FFMpegProcess p = new FFMpegProcess.Builder()
            .addAudio("u8", 50, 1)
                .setStreamTag("name", "acceleration")
                .setStreamTag("location", "hip")
            .addAudio("u8", 1, 4)
                .setStreamTag("name", "gps")
                .setStreamTag("location", "hip")
            .addVideo(320,240,10,"rawvideo", "nv21")
                .setStreamTag("name", "frontcam")
            .setCodec("a", "wavpack")
            .setCodec("v", "libtheora")
            .addOutputArgument("-qscale:v", "7")
            .setOutput(new File(filepath, filename).toString(), "matroska")
            .build(c);

        Thread.sleep(250); p.getOutputStream(0).write(b);
        Thread.sleep(250); p.getOutputStream(1).write(b);
        Thread.sleep(250); p.getOutputStream(2).write(a);

        int e = p.terminate();

        Assert.assertTrue("ffmpeg exited cleanly", e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());

        /** now check if the streams were properly encoded */
        FFProbeProcess pp = new FFProbeProcess.Builder()
                .addInput(filename)
                .addShowOption("streams")
                .build(c);
        JSONArray streams = pp.getJSONResult().getJSONArray("streams");

        System.err.println(streams);

        Assert.assertTrue("one stream", streams.length()==3);

        Assert.assertTrue("is wavpacked", streams.getJSONObject(0).getString("codec_name").equals("wavpack"));
        Assert.assertTrue( "sample rate", streams.getJSONObject(0).getDouble("sample_rate") == 50);
        Assert.assertTrue(    "channels", streams.getJSONObject(0).getDouble("channels") == 1);
        Assert.assertEquals("correct duration", "00:01:21.920000000", streams.getJSONObject(0)
                                                    .getJSONObject("tags").getString("DURATION"));

        Assert.assertTrue("is wavpacked", streams.getJSONObject(1).getString("codec_name").equals("wavpack"));
        Assert.assertTrue( "sample rate", streams.getJSONObject(1).getDouble("sample_rate") == 1);
        Assert.assertTrue(    "channels", streams.getJSONObject(1).getDouble("channels") == 4);
        Assert.assertEquals("correct duration", "00:17:04.000000000", streams.getJSONObject(1)
                                                    .getJSONObject("tags").getString("DURATION"));

        Assert.assertTrue(       "width", streams.getJSONObject(2).getDouble("width") == 320);
        Assert.assertTrue(      "height", streams.getJSONObject(2).getDouble("height") == 240);
        Assert.assertTrue(  "is theorad", streams.getJSONObject(2).getString("codec_name").equals("theora"));
        Assert.assertEquals("frame rate", "10/1", streams.getJSONObject(2).getString("avg_frame_rate"));
        Assert.assertEquals("duration is correct", "00:00:01.000000000", streams.getJSONObject(2)
                                                    .getJSONObject("tags").getString("DURATION"));
    }
}
