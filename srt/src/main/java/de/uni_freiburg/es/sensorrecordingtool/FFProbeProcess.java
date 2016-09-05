package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/** A wrapper for the ffprobe command.
 *
 * Created by phil on 9/5/16.
 */
public class FFProbeProcess {
    protected static final ExecutorService THREAD_POOL_EXECUTOR = Executors.newCachedThreadPool();
    protected final Process p;
    protected final AsyncTask<InputStream, Void, Void> verbose =
            new AsyncTask<InputStream, Void, Void>() {
                @Override
                protected Void doInBackground(InputStream... ps) {
                    InputStream is = ps[0];

                    try {
                        byte buf[] = new byte[4096];

                        while(true) {
                            int n = is.read(buf);
                            if (n<=0) break;
                            System.err.write(buf, 0, n);
                        }
                    } catch (IOException e) {}
                    return null;
                }};

    protected FFProbeProcess(ProcessBuilder pb) throws IOException {
        p = pb.start();
        verbose.executeOnExecutor(THREAD_POOL_EXECUTOR, p.getErrorStream());
    }

    public String getResult() throws InterruptedException, IOException {
        p.waitFor();
        byte buf[] = new byte[p.getInputStream().available()];
        int n = p.getInputStream().read(buf);
        return new String(buf, 0, n);
    }

    public JSONObject getJSONResult() throws IOException, InterruptedException, JSONException {
        return new JSONObject(getResult());
    }

    public int waitFor() throws InterruptedException { return p.waitFor(); }


    /** This is a builder for the ffprobe commad.
     *
     */
    public static class Builder {
        public LinkedList<String> cmdline = new LinkedList<>();
        public String print_format = "json";

        public Builder addInput(String input) {
            cmdline.add("-i");
            cmdline.add(input);
            return this;
        }

        public Builder setFormat(String format) {
            print_format = format;
            return this;
        }

        public Builder addShowOption(String format) {
            cmdline.add(String.format("-show_%s", format));
            return this;
        }

        public Builder addArgument(String key, String value) {
            cmdline.add(key);
            cmdline.add(value);
            return this;
        }

        public FFProbeProcess build(Context c) throws IOException {
            LinkedList<String> args = (LinkedList<String>) cmdline.clone();
            File path = new File(new File(c.getFilesDir().getParentFile(), "lib"), "libffprobe.so");
            args.add(0, path.toString());
            args.add("-print_format");
            args.add(print_format);

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));

            return new FFProbeProcess(pb);
        }
    }
}
