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
    private boolean mClosed;

    public EventualSocketOutputStream(String host, int port) {
        mHost = host;
        mPort = port;
        mOutS = new ByteArrayOutputStream();
        mSock = null;
        mNumBytes = 0;
        mClosed = false;
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
        mOutS.write(buffer);

        if (mSock == null || !(mSock.isConnected() || mSock.isClosed())) try {
            byte[] buf;
            mSock = new Socket();
            mSock.connect(new InetSocketAddress("localhost", mPort), 500);
            buf = ((ByteArrayOutputStream) mOutS).toByteArray();
            mOutS = new BufferedOutputStream(mSock.getOutputStream());
            mOutS.write(buf);

            if (mClosed)
                mSock.close();
        } catch (SocketTimeoutException e) {
        } catch (ConnectException e) {
        }
    }

    @Override
    public void close() throws IOException {
        mClosed = true;
        if (mSock != null)
            mSock.close();
    }
}
