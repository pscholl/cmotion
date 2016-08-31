package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;

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

        Assert.assertTrue(new String(b,0,i), e==1);
        Assert.assertTrue("ffprobe in output", new String(b, 0,i).contains("ffprobe"));
    }

    @Test public void encodeSomeNumbersIntoSingleChannelWavpacked() throws IOException, InterruptedException {
        byte[] b = new byte[4096];
        FFMpegProcess p = FFMpegProcess.ffmpeg(c, "-f u8 -i tcp://localhost:%port?listen -f wv -y "+filename);

        p.getOutputStream(0).write(b);
        p.getOutputStream(0).close();

        int e = p.waitFor();
        int i = p.getErrorStream().read(b);

        Assert.assertTrue(new String(b,0,i), e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());
    }

    @Test public void encodeSomeNumbersIntoSingleChannelMatroska() throws IOException, InterruptedException {
        byte[] b = new byte[4096];
        FFMpegProcess p = FFMpegProcess.ffmpeg(c, "-f u8 -i tcp://localhost:%port?listen -f matroska -y "+filename);

        p.getOutputStream(0).write(b);
        p.getOutputStream(0).close();

        int e = p.waitFor();
        int i = p.getErrorStream().read(b);

        Assert.assertTrue(new String(b,0,i), e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());
    }

    @Test public void encodeMultipleIntoMatroska() throws IOException, InterruptedException {
        byte[] b = new byte[4096];
        FFMpegProcess p = FFMpegProcess.ffmpeg(c,
                "-ar 50  -f u8 -i tcp://localhost:%port?listen " +
                "-ar 1   -f u8 -i tcp://localhost:%port?listen " +
                "-ar 100 -f u8 -i tcp://localhost:%port?listen " +
                "-c:a wavpack -map 0 -map 1 -map 2 -f matroska -y "+filename);

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


        p.getOutputStream(0).close();
        p.getOutputStream(1).close();
        p.getOutputStream(2).close();

        int e = p.waitFor();
        int i = p.getErrorStream().read(b);

        Assert.assertTrue(new String(b,0,i), e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());
    }

    @Test public void encodeWithBuilder() throws Exception {
        byte[] b = new byte[4096];
        FFMpegProcess p = FFMpegProcess.builder()
            .addAudio("u8", 50)
                .setStreamTag("name", "acceleration")
                .setStreamTag("location", "hip")
            .addAudio("u8", 1)
                .setStreamTag("name", "gps")
                .setStreamTag("location", "hip")
            .setCodec("a", "wavpack")
            .setOutput(new File(filepath, filename).toString(), "matroska")
            .build(c);

        p.getOutputStream(0).write(b);
        p.getOutputStream(1).write(b);

        p.getOutputStream(0).close();
        p.getOutputStream(1).close();

        int e = p.waitFor();
        int i = p.getErrorStream().read(b);

        Assert.assertTrue(new String(b,0,i), e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());
    }

    @Test public void encodeWithBuilderVideo() throws Exception {
        byte[] b = new byte[4096],
               a = new byte[10*320*240*12/8]; // nv21 has 12bit per pixel

        FFMpegProcess p = FFMpegProcess.builder()
            .addAudio("u8", 50)
                .setStreamTag("name", "acceleration")
                .setStreamTag("location", "hip")
            .addAudio("u8", 1)
                .setStreamTag("name", "gps")
                .setStreamTag("location", "hip")
            .addVideo(320,240,10,"rawvideo", "nv21")
                .setStreamTag("name", "frontcam")
            .setCodec("a", "wavpack")
            .addOutputSwitch("-c:v", "libtheora")
            .addOutputSwitch("-qscale:v", "7")
            .setOutput(new File(filepath, filename).toString(), "matroska")
            .build(c);

        p.getOutputStream(0).write(b);
        p.getOutputStream(1).write(b);
        p.getOutputStream(2).write(a);

        int e = p.terminate();
        int i = p.getErrorStream().read(b);

        Assert.assertTrue(new String(b,0,i), e==0);
        Assert.assertTrue(new File(filepath, filename).isFile());
    }
}
