package es.uni_freiburg.de.cmotion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This class queues and transports byte[] as single UDP packages over the local network
 * to interested parties (also to those which are not, it's called broadcasting).
 *
 * Created by phil on 1/5/16.
 */
public class UDPTransport extends Thread {
    private InetAddress mAdress;
    private int mPort;
    private BlockingDeque<byte[]> q;
    private static UDPTransport mInstance;
    private boolean mIsSending = true;

    /*
     * create a new instance of this Transport, multiple can be created and started
     * or you use the getInstance() method to get a singleton of this transport.
     */
    public UDPTransport() {
        try {
            q = new LinkedBlockingDeque<>(1024);
            mAdress = InetAddress.getByName("255.255.255.255");
            mPort = 5050;
            new Thread(mUDPSender).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /*
     * A Runnable that will block on q, which contains readily available ByteBuffers which
    * are to be sent to mAddress and mPort. This will only be done while mSending is true.
     */
    private Runnable mUDPSender = new Runnable() {

        public DatagramPacket packet;
        public DatagramSocket socket;

        @Override
        public void run() {
            while (mIsSending) {
                try {
                    byte[] buf = q.takeLast();
                    packet.setData(buf);
                    packet.setLength(buf.length);
                    socket.send(packet);
                } catch (Exception e) {
                    try {
                        socket = new DatagramSocket();
                        packet = new DatagramPacket(new byte[]{}, 0, mAdress, mPort);
                        e.printStackTrace();
                    } catch (SocketException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    };

    /*
     * convenience when aggregating all channels.
     */
    public static UDPTransport getInstance() {
        if (mInstance == null)
            mInstance = new UDPTransport();
        return mInstance;
    }

    /*
     * enqueues one buffer for sending, return false if there is no space in the queue and the
     * packet has been dropped.
     */
    public boolean send(byte[] buf) {
        try {
            q.add(buf);
            return true;
        } catch(IllegalStateException e) {
            q.removeFirst();
            return false;
        }
    }

}
