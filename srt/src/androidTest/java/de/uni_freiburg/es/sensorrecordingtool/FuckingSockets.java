package de.uni_freiburg.es.sensorrecordingtool;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Assert;
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
public class FuckingSockets extends BroadcastingTest {

    @Test
    public void testUnixSocket() throws Exception {
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            LocalSocket s = new LocalServerSocket("testsocket").accept();
            System.err.println("sucess");
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      t.start();
      Thread.sleep(550);

      LocalSocket s = new LocalSocket(LocalSocket.SOCKET_STREAM);
      s.connect(new LocalSocketAddress("testsocket"));
    }
}
