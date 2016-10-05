package de.uni_freiburg.es.sensorrecordingtool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

/** Buffer all writes until the socket is connected, then send the buffer and afterwards directly
 * communicate on the socket's outputstream.
 *
 * Created by phil on 10/5/16.
 */
public class EventualSocketOutputStream extends OutputStream {
    private final String mHost;
    private final int mPort;
    private ByteArrayOutputStream mBuff;
    private Socket mSock;

    public EventualSocketOutputStream(String host, int port) {
        mHost = host;
        mPort = port;
        mBuff = new ByteArrayOutputStream();
        mSock = null;
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
        if (mSock == null) {
            try {
                mSock = new Socket(mHost, mPort);
                mSock.getOutputStream().write(mBuff.toByteArray());
                mBuff = null;
            } catch (ConnectException e) {
                mBuff.write(buffer);
            }
        } else {
            mSock.getOutputStream().write(buffer);
        }
    }

    @Override
    public void close() throws IOException {
        if (mSock != null)
            mSock.close();
    }
}
