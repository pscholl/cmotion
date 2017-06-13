package de.uni_freiburg.es.sensorrecordingtool;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;

import static org.junit.Assert.assertEquals;

/**
 * Created by paulgavrikov on 13.06.17.
 */

public class SubRipTest {

    @Test
    public void testTimeEncoding() {
        assertEquals("01:02:03,456", new SubRip.Builder().formatTime(3723456));
    }

    @Test
    public void testSingleLine() {

        String subRip = new SubRip.Builder()
                .addLine(0, 100, "Hello World")
                .create();

        assertEquals("1\n00:00:00,000 --> 00:00:00,100\nHello World\n\n", subRip);
    }

    @Test
    public void testSortedMultiLine() {

        String subRip = new SubRip.Builder()
                .addLine(0, 100, "Hello World")
                .addLine(1000, 1100, "What a great test")
                .create();

        assertEquals("1\n00:00:00,000 --> 00:00:00,100\nHello World\n\n2\n00:00:01,000 --> 00:00:01,100\nWhat a great test\n\n", subRip);
    }

    @Test
    public void testUnsortedMultiLine() {

        String subRip = new SubRip.Builder()
                .addLine(1000, 1100, "What a great test")
                .addLine(0, 100, "Hello World")
                .create();

        assertEquals("1\n00:00:00,000 --> 00:00:00,100\nHello World\n\n2\n00:00:01,000 --> 00:00:01,100\nWhat a great test\n\n", subRip);
    }

    @Test
    public void testWriteRead() throws Exception{
        String subRip = new SubRip.Builder()
                .addLine(0, 100000000, "University of Freiburg")
                .create();

        BufferedWriter bw = new BufferedWriter(new FileWriter("/sdcard/test.srt"));
        bw.write(subRip);
        bw.flush();
        bw.close();
    }
}
