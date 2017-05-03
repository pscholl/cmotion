package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FFMpegCopyProcess {
    protected FFMpegCopyProcess.ExitCallback exit;
    protected static final ExecutorService THREAD_POOL_EXECUTOR = Executors.newCachedThreadPool();
    protected final Process p;
    protected final ProcessBuilder pb;
    protected final AsyncTask<InputStream, Void, Void> verboseMonitor =
        new AsyncTask<InputStream, Void, Void>() {
        @Override
        protected Void doInBackground(InputStream... ps) {
            InputStream is = ps[0];

            try {
                byte buf[] = new byte[4096];

                while(!isCancelled()) {
                    int n = is.read(buf);
                    System.err.write(buf, 0, n);
                }
            } catch (IOException e) {}
            return null;
    }};
    protected final AsyncTask<Process, Void, Void> exitMonitor = new AsyncTask<Process, Void, Void>() {
        @Override
        protected Void doInBackground(Process... ps) {
            Process p = ps[0];
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (exit != null)
                exit.processDone();
            return null;
    }};


    protected FFMpegCopyProcess(ProcessBuilder process) throws IOException {
        pb = process;
        p = pb.start();
        System.err.println("executing " + pb.command().toString());
        verboseMonitor.executeOnExecutor(THREAD_POOL_EXECUTOR, p.getErrorStream());
        exitMonitor.executeOnExecutor(THREAD_POOL_EXECUTOR, p);
    }

    public int waitFor() throws InterruptedException {
        int ret = p.waitFor();
        verboseMonitor.cancel(true);
        return ret;
    }

    public InputStream getErrorStream() { return p.getErrorStream();  }

    public int terminate() throws InterruptedException {
        return p.waitFor();
    }

    public InputStream getInputStream() {
        return p.getInputStream();
    }

    public void exitCallback(FFMpegCopyProcess.ExitCallback cb) {
        this.exit = cb;
    }


    /** This is a helper class to build what my common usages for the FFMpeg tool will be, feel
     * free to add additional stuff here. You can always add your own command line switches with
     * the addSwitch() function.
     */
    public static class Builder {
        private String output;
        private String[] input;


        public Builder setOutput(String output) throws Exception {
            if (output == null)
                throw new Exception("output must be non-null");
            this.output = new File(output).exists() && !output.startsWith("file:") ?
                          "file:"+output : output;

            return this;
        }

        public Builder setInput(String... input) throws Exception {
            if (input == null)
                throw new Exception("input must be non-null");

            this.input = input;

            for(int i = 0; i<input.length; i++) {
                this.input[i] = new File(this.input[i]).exists() && !this.input[i].startsWith("file:") ?
                        "file:" + this.input[i] : this.input[i];

            }

            return this;
        }

        public FFMpegCopyProcess build(Context c) throws IOException {
            LinkedList<String> cmdline = new LinkedList<String>();
            File path = new File(new File(c.getFilesDir().getParentFile(), "lib"), "libffmpeg.so");

            cmdline.add(path.toString());

            for(String file : input) {
                cmdline.add("-i");
                cmdline.add(file);
            }

            cmdline.add("-c");
            cmdline.add("copy");

            for(int i=0; i<input.length;i++) {
                cmdline.add("-map");
                cmdline.add(i+"");
                cmdline.add("-map_metadata");
                cmdline.add(i+"");

            }

            cmdline.add(output);
            ProcessBuilder pb = new ProcessBuilder(cmdline);
            pb.directory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
            return new FFMpegCopyProcess(pb);
        }
    }

    public interface ExitCallback {
        public void processDone();
    }
}
