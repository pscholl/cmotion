package de.uni_freiburg.es.sensorrecordingtool;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;

/**
 * Created by phil on 9/1/16.
 */
public class AsyncSocket extends Socket {
    private final Integer port;
    private final String host;
    private final AsyncTask<Void, Void, Void> async;
    private Socket socket;
    private boolean interrupted = false;
    private PipedInputStream pipein;
    private PipedOutputStream pipeout;
    private boolean connected = false;

    public AsyncSocket(final String host, final Integer port) throws IOException {
        this.host = host;
        this.port = port;
        this.pipein = new PipedInputStream();
        this.pipeout = new PipedOutputStream();
        this.pipein.connect(this.pipeout);

        this.async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    byte buf[] = new byte[4096];
                    for (int t=0; t<2000; t+=5) {
                        try { AsyncSocket.this.socket = new Socket(host, port); break; }
                        catch (Exception e ) { e.printStackTrace(); sleep(5); }
                    }
                    AsyncSocket.this.connected = true;
                    while (!AsyncSocket.this.interrupted) {
                        int n = AsyncSocket.this.pipein.read(buf, 0, buf.length);
                        if (n>1) socket.getOutputStream().write(buf, 0, n);
                        else     break;
                    }
                    socket.getOutputStream().close();
                    pipeout.close();
                } catch (IOException e) { e.printStackTrace(); }
                return null;
            }

            private void sleep(int ms) {
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        this.async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public OutputStream getOutputStream() {
        return this.pipeout;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
