package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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

    protected FFMpegProcess(ProcessBuilder process) throws IOException {
        boolean isinputarg = false;
        LinkedList<String> newargs;
        LinkedList<Integer> t;

        pb = process;
        t = new LinkedList<Integer>();
        sockets = new LinkedList<Socket>();
        newargs = new LinkedList<String>();


        for (String arg : pb.command()) {
            if (isinputarg) {
                Integer port = getFreeTCPPort();
                arg = arg.replaceFirst("%port", port.toString());
                t.add(port);
            }

            isinputarg = arg.contentEquals("-i");
            newargs.add(arg);
        }

        pb.command(newargs);
        p = pb.start();

        for (int i=0, time=0; i<t.size() && time<2000; time+=5)
            try { sockets.push(new Socket("localhost", t.get(i))); i++; }
            catch (Exception e) { sleep(5); }
    }

    public OutputStream getOutputStream(int i) throws IOException {
        return sockets.get(i).getOutputStream();
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

    private static LinkedList getCommand(File path, String[] args) {
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
}
