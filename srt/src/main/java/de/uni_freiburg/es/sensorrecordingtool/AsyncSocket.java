package de.uni_freiburg.es.sensorrecordingtool;

import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/** It ain't pretty
 *
 * Created by phil on 9/1/16.
 */
public class AsyncSocket extends OutputStream {
    private final Integer port;
    private final String host;
    private final AsyncTask<Void, Void, Void> async;
    private int numbytes;
    private Socket socket;
    private ByteArrayOutputStream os;
    private LinkedBlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
    private boolean isclosed = false;

    public AsyncSocket(final String host, final Integer port) throws IOException {
        this.host = host;
        this.port = port;
        this.os = new ByteArrayOutputStream();
        this.numbytes = 0;

        this.async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    for (int t=0; t<2000; t+=5) {
                        try {
                            socket = new Socket(host, port);
                            break;
                        } catch (Exception e ) { sleep(5); }
                    }

                    while (socket != null && socket.isConnected() && (!isclosed || q.size()!=0)) {
                        byte buf[] = q.take();
                        numbytes += buf.length;
                        socket.getOutputStream().write(buf);
                        System.err.println(String.format("written %d bytes on %d", numbytes, port));
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    System.err.println(String.format("closing port %d", port));
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

    @Override
    public void write(byte buf[]) {
        q.add(buf);
    }

    @Override
    public void write(int i) throws IOException {

    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        q.add(buffer);
    }

    @Override
    public void close() throws IOException {
        isclosed = true;
    }
}
