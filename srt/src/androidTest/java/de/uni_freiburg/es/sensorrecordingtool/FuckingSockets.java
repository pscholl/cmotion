package de.uni_freiburg.es.sensorrecordingtool;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by phil on 10/6/16.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FuckingSockets {
    @Test
    public void testSocket() throws IOException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(1992);
                    ss.accept();
                    System.err.println("success");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();

        Socket s = new Socket();
        s.connect(new InetSocketAddress("localhost", 1992), 500);
    }
}
