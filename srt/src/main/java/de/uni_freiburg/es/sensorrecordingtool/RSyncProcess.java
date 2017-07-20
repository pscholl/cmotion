package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RSyncProcess {
    protected RSyncProcess.ExitCallback exit;
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

                        while (!isCancelled()) {
                            int n = is.read(buf);
                            System.err.write(buf, 0, n);
                        }
                    } catch (IOException e) {
                    }
                    return null;
                }
            };
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
        }
    };


    protected RSyncProcess(ProcessBuilder process) throws IOException {
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

    public InputStream getErrorStream() {
        return p.getErrorStream();
    }

    public int terminate() throws InterruptedException {
        return p.waitFor();
    }

    public InputStream getInputStream() {
        return p.getInputStream();
    }

    public void exitCallback(RSyncProcess.ExitCallback cb) {
        this.exit = cb;
    }


    /**
     * This is a helper class to build what my common usages for the FFMpeg tool will be, feel
     * free to add additional stuff here. You can always add your own command line switches with
     * the addSwitch() function.
     */
    public static class Builder {
        private String output;
        private String input;
        private ArrayList<String> params  = new ArrayList<>();

        public Builder setOutput(String output) throws Exception {
            if (output == null)
                throw new Exception("output must be non-null");
            this.output = output;
            return this;
        }

        public Builder showProgress() {
            params.add("--progress");
            return this;
        }

        public Builder showStats() {
            params.add("--stats");
            return this;
        }

        public Builder dryRun() {
            params.add("-n");
            return this;
        }


        public Builder setInput(String input) throws Exception {
            if (input == null)
                throw new Exception("input must be non-null");
            this.input = input;
            return this;
        }

        public RSyncProcess build(Context c) throws IOException {
            LinkedList<String> cmdline = new LinkedList<String>();
            File dir = c.getFilesDir().getParentFile();
            File path = new File(new File(dir, "lib"), "librsync.so");

            cmdline.add(path.toString());

            cmdline.addAll(params);

            cmdline.add(input);
            cmdline.add(output);
            ProcessBuilder pb = new ProcessBuilder(cmdline);
            return new RSyncProcess(pb);
        }
    }

    public interface ExitCallback {
        public void processDone();
    }
}
