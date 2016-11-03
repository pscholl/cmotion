package de.uni_freiburg.es.sensorrecordingtool;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

/** Buffer all writes until the socket is connected, then send the buffer and afterwards directly
 * communicate on the socket's outputstream.
 *
 * Created by phil on 10/5/16.
 */
public class EventualSocketOutputStream extends OutputStream {
    private final String mHost;
    private final int mPort;
    private OutputStream mOutS;
    private Socket mSock;
    private int mNumBytes;
    private boolean mClosed, mWasConnected;
    private final long mTimeout;

    public EventualSocketOutputStream(String host, int port) {
      this(host, port, 0);
    }

    public EventualSocketOutputStream(String host, int port, long timeout_ms) {
        mHost = host;
        mPort = port;
        mTimeout = timeout_ms;
        mOutS = new ByteArrayOutputStream();
        mSock = null;
        mNumBytes = 0;
        mClosed = false;
        mWasConnected = false;
    }

    @Override
    public void write(int i) throws IOException {
        throw new IOException("not implemented");
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        throw new IOException("not implemented");
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        eventuallyConnectSocketAndFlush();
        mOutS.write(buffer);
    }

    public void eventuallyConnectSocketAndFlush() {
        if (mSock == null || !(mSock.isConnected() || mSock.isClosed())) try {
            byte[] buf;
            mSock = new Socket();
            System.err.printf("waiting for port %d\n", mPort);
            mSock.connect(new InetSocketAddress("localhost", mPort));
            mWasConnected = true;
            buf = ((ByteArrayOutputStream) mOutS).toByteArray();
            mOutS = mSock.getOutputStream();
            mOutS.write(buf);
            System.err.printf("written %d bytes on %d\n", buf.length, mPort);
        } catch (Exception e) {
          mSock = null;
          System.err.printf("unable to write on %d\n", mPort);
        }
    }

    @Override
    public void close() throws IOException {
        /* try a little bit harder if the socket was never open */
        if (!mWasConnected)
          System.err.printf("port %d was %b %d bytes left\n", mPort, mWasConnected, ((ByteArrayOutputStream) mOutS).toByteArray().length);
        else
          System.err.printf("port %d was %b\n", mPort, mWasConnected);

        for (long ticks = 0; !mWasConnected && (mTimeout == 0 || ticks < mTimeout); ticks += 10) {
          eventuallyConnectSocketAndFlush();
          try { Thread.sleep(10); }
          catch(InterruptedException e) {}
        }

        eventuallyConnectSocketAndFlush();

        if (mSock != null) {
            mOutS.close();
            mSock.close();
        }
    }
}
