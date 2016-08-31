package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;

/**
 * This is a wrapper for FFMpeg that allows to run ffmpeg executable and returns Process
 * object to interact with the running ffmpeg process. As java cannot do pipes, we use tcp
 * streams to handle multiple input files. For this ffmpeg and ffprobe can be called with
 * a %port special string that will be replaced with a free port on the system. I.e.
 *
 *  FFMpegProcess.ffmpeg("ffmpeg -f u8 -i tcp://localhost:%port?list") will replace %port with
 *  a free TCP port on the system prior to execution.
 *
 * Created by phil on 8/26/16.
 */
public class FFMpegProcess {

    protected final Process p;
    protected final ProcessBuilder pb;
    protected final LinkedList<Socket> sockets;
    protected final LinkedList<Integer> ports;

    protected FFMpegProcess(ProcessBuilder process) throws IOException {
        boolean isinputarg = false;
        LinkedList<String> newargs;

        pb = process;
        ports = new LinkedList<Integer>();
        sockets = new LinkedList<Socket>();
        newargs = new LinkedList<String>();

        for (String arg : pb.command()) {
            if (isinputarg) {
                Integer port = getFreeTCPPort();
                arg = arg.replaceFirst("%port", port.toString());
                ports.add(port);
            }

            isinputarg = arg.contentEquals("-i");
            if (arg.trim().length() !=0 )
                newargs.add(arg.trim());
        }

        pb.command(newargs);
        p = pb.start();
    }

    public OutputStream getOutputStream(int i) throws IOException, InterruptedException {
        try {
            return sockets.get(i).getOutputStream();
        } catch (IndexOutOfBoundsException e) {
            if (sockets.size() != i)
                throw new IOException("need to retrieve outputs streams in order");

            Integer port = ports.get(i);
            for (int t=0; t<2000; t+=5) {
                try { sockets.add(new Socket("localhost", port)); break; }
                catch (ConnectException e1) { Thread.sleep(5); }
            }

            if (sockets.size()-1 != i) { // ffmpeg failed to start
                byte buf[] = new byte[4096];
                int n = p.getErrorStream().read(buf);
                throw new IOException(new String(buf, 0, n));
            }

            return sockets.get(i).getOutputStream();
        }
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (Exception e) {}
    }

    private Integer getFreeTCPPort() {
        int port = 0;

        try {
            ServerSocket s = new ServerSocket(0);
            port = s.getLocalPort();
            s.close();
        } catch (IOException e) {e.printStackTrace();}

        return port;
    }

    protected static LinkedList getCommand(File path, String[] args) {
        LinkedList tokens = new LinkedList<String>();

        tokens.add(path.toString());
        if (args.length == 1)
            Collections.addAll(tokens, args[0].split(" "));
        else if (args.length > 1)
            Collections.addAll(tokens, args);

        return tokens;
    }

    public static FFMpegProcess ffmpeg(Context c, String... args) throws IOException {
        File path = new File(new File(c.getFilesDir().getParentFile(), "lib"), "libffmpeg.so");
        ProcessBuilder pb = new ProcessBuilder(getCommand(path,args));
        pb.directory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
        return new FFMpegProcess(pb);
    }

    public static FFMpegProcess ffprobe(Context c, String... args) throws IOException {
        File path = new File(new File(c.getFilesDir().getParentFile(), "lib"), "libffprobe.so");
        ProcessBuilder pb = new ProcessBuilder(getCommand(path,args));
        pb.directory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
        return new FFMpegProcess(pb);
    }

    public int waitFor() throws InterruptedException {
        int ret = p.waitFor();

        for (Socket s : sockets)
            try { s.close(); }
            catch (IOException e) {}

        return ret;
    }

    public InputStream getErrorStream() { return p.getErrorStream();  }

    public static FFMpegProcessBuilder builder() {
        return new FFMpegProcessBuilder();
    }

    public int terminate() throws InterruptedException {
        for (Socket s : sockets)
            try { s.close(); }
            catch (IOException e) {  }

        return p.waitFor();
    }

    /** This is a helper class to build what my common usages for the FFMpeg tool will be, feel
     * free to add additional stuff here. You can always add your own command line switches with
     * the addSwitch() function.
     */
    protected static class FFMpegProcessBuilder  {
        LinkedList<String> inputopts = new LinkedList<String>(),
                          outputopts = new LinkedList<String>();
        int numinputs  = 0;

        /** add an audio stream to the ffmpeg input
         *
         * @param format sample format, list them with ffmpeg -formats or documentation
         * @param rate   sample rate in Hz
         */
        public FFMpegProcessBuilder addAudio(String format, int rate) {
            Collections.addAll(inputopts, String.format(
                "-f %s -ar %d -i tcp://localhost:%%port?listen",format, rate).split(" "));

            numinputs ++;
            return this;
        }

        /** add a video stream to the ffmpeg input
         *
         * @param width   width of the input video
         * @param height  height of the input video
         * @param rate    input rate of the video stream
         * @param fmt     video format, e.g. raw
         * @param pixfmt  pixel format for the video stream, list the available ones with pix_fmt,
         *                set to null if specified by the input format. The default for Android is
         *                NV21.
         */
        public FFMpegProcessBuilder addVideo(int width, int height, int rate, String fmt, String pixfmt) {
            String optarg = pixfmt == null ? "" : String.format("-pix_fmt %s", pixfmt);

            Collections.addAll(inputopts,
                String.format("-r %d -s %d:%d -f %s %s -i tcp://localhost:%%port?listen",
                       rate, width, height, fmt, optarg).split(" "));

            numinputs ++;
            return this;
        }

        /** set a metadata tag for the last defined input stream
         *
         * @param key name of tag to set
         * @param value value of the specified tag
         */
        public FFMpegProcessBuilder setStreamTag(String key, String value) throws Exception {
            if (numinputs == 0)
                throw new Exception("no stream to apply tags to, please add one first");

            outputopts.add(String.format("-metadata:s:%d", numinputs-1));
            outputopts.add(String.format("%s=%s", key, value));

            return this;
        }

        /** set a metadata tag for the whole output file
         *
         * @param key name of the tag
         * @param value value of the tag
         */
        public FFMpegProcessBuilder setTag(String key, String value) {
            outputopts.add("-metadata");
            outputopts.add(String.format("%s=%s", key, value));

            return this;
        }

        /** set the output codec for the current stream. In case this is not set the default
         * for the output format will be used.
         *
         * @param codec set the codec to encode the output with
         */
        public FFMpegProcessBuilder setStreamCodec(String codec) throws Exception {
            if (numinputs == 0)
                throw new Exception("no stream to apply tags to, please add one first");

            outputopts.add(String.format("-c:%d", numinputs-1));
            outputopts.add(codec);

            return this;
        }

        /** set the codec to use for a given stream specifier, see the ffmpeg manpage for details
         *
         * @param stream stream specifier
         * @param codec audio codec to use
         */
        public FFMpegProcessBuilder setCodec(String stream, String codec) {
            outputopts.add(String.format("-c:%s", stream));
            outputopts.add(codec);
            return this;
        }

        /** add a cmdline switch
         *
         * @param option include the leading dash to the command line option
         * @param value the value of this option
         */
        public FFMpegProcessBuilder addOutputSwitch(String option, String value) {
            outputopts.add(option);
            outputopts.add(value);

            return this;
        }

        /** add a cmdline switch
         *
         * @param option include the leading dash to the command line option
         * @param value the value of this option
         */
        public FFMpegProcessBuilder addInputSwitch(String option, String value) {
            inputopts.add(option);
            inputopts.add(value);

            return this;
        }

        /** set the output, per default all inputs will be mapped into the output. If you need a
         * different behaviour, specify the "-map" and output option with the addSwitch method.
         *
         * @param output the output file, channel etc. that ffmpeg supports (defaults to overwriting)
         * @param format set to null if ffmpeg is to decide, otherwise choose a container
         */
        public FFMpegProcessBuilder setOutput(String output, String format) {
            for (int i=0; i<numinputs; i++) {
                outputopts.add("-map");
                outputopts.add(String.format("%d", i));
            }
            outputopts.add("-f");
            outputopts.add(format);
            outputopts.add("-y");
            outputopts.add(output);

            return this;
        }

        public FFMpegProcess build(Context c) throws IOException {
            LinkedList<String> cmdline = new LinkedList<String>();
            File path = new File(new File(c.getFilesDir().getParentFile(), "lib"), "libffmpeg.so");
            cmdline.add(path.toString());
            cmdline.addAll(inputopts);
            cmdline.addAll(outputopts);
            ProcessBuilder pb = new ProcessBuilder(cmdline);
            pb.directory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
            return new FFMpegProcess(pb);
        }
    }
}
