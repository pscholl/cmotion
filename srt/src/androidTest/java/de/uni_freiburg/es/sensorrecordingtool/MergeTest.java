package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.merger.IOUtils;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeConst;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeService;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeSession;
import de.uni_freiburg.es.sensorrecordingtool.merger.provider.WearDataProvider;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.DataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.LocalDataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.WearDataRetriever;

@RunWith(AndroidJUnit4.class)
public class MergeTest extends BroadcastingTest {
    private Context c;
    private GoogleApiClient mGoogleApiClient;
    final File testFile = createTestFile();
    final File testFile2 = createTestFile();
    String ownId;


    @Before
    public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        ownId = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.blockingConnect();
    }

    @Test public void testMergeSessionTimeout() throws InterruptedException {

        ArrayList<String> readyNodes = new ArrayList<>();
        final Node n1 = new Node("platform", "someNodeUUID1");
        final Node n2 = new Node("platform", "someNodeUUID2");

        n1.setConnectionTechnologies(new ConnectionTechnology[] {new ConnectionTechnology(ConnectionTechnology.Type.LOCAL)});
        n2.setConnectionTechnologies(new ConnectionTechnology[] {new ConnectionTechnology(ConnectionTechnology.Type.BT_CLASSIC)});

        n1.setAvailableSensors(new String[] {"sensor"});
        n2.setAvailableSensors(new String[] {"sensor"});

        n1.setReadySensors(new String[] {"sensor"});
        n2.setReadySensors(new String[] {"sensor"});

        n1.setDrift(0);
        n2.setDrift(0);


        AutoDiscovery.getInstance(c).setDiscoveredSensors(new ArrayList<Node>() {
            {
                add(n1); add(n2);
            }
        });

        readyNodes.add(n1.getAid());
        readyNodes.add(n2.getAid());

        Intent startServiceIntent = new Intent(c, MergeService.class);
        startServiceIntent.putExtra(RecorderStatus.RECORDING_UUID, "testUUID");
        startServiceIntent.putExtra(MergeService.RELEVANT_AIDS, readyNodes);
        c.startService(startServiceIntent);

        Thread.sleep(5000);

        Intent intent = new Intent(RecorderStatus.FINISH_ACTION);
        intent.putExtra(RecorderStatus.RECORDING_UUID, "testUUID");
        intent.putExtra(RecorderStatus.FINISH_PATH, "/sdcard/test.mkv");
        c.sendBroadcast(intent);

        Thread.sleep(MergeSession.TIMEOUT_AFTER_LAST_FILE_MS);
        Thread.sleep(500); // give some more time as intents can be a bit delayed

        // assert merge closed.

//        while(true);

    }

    @Test
    public void testWearProviderChunkRaw() throws InterruptedException {
        final WearDataProvider provider = new WearDataProvider(c);
        final CountDownLatch latch = new CountDownLatch(2);
        Wearable.DataApi.addListener(mGoogleApiClient, new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEventBuffer) {
                for (DataEvent event : dataEventBuffer) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        // DataItem changed
                        DataItem item = event.getDataItem();
                        if (item.getUri().getPath().equals("/test/test")) {
                            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                            Assert.assertTrue(dataMap.getByteArray(MergeConst.KEY_DATA).length == MergeConst.CHUNK_SIZE);
                            Assert.assertTrue(dataMap.getInt(MergeConst.KEY_TOTAL) == 2);
                            latch.countDown();
                        }
                    }
                }
            }
        });

        byte[] data = new byte[MergeConst.CHUNK_SIZE];
        Arrays.fill(data, (byte) 64);
        provider.serveChunk("/test/test", data, 0, 2);
        provider.serveChunk("/test/test", data, 1, 2);
        latch.await();
    }

    /**
     * Test Chunk transmissions via Wear provider and receiver, also test for independence of AndroidID
     *
     * @throws InterruptedException
     */
    @Test
    public void testWearProviderChunkWithReceiver() throws InterruptedException {
        final WearDataProvider provider = new WearDataProvider(c);
        final CountDownLatch latch = new CountDownLatch(2);
        final WearDataRetriever retriever = new WearDataRetriever(c, new Node("platform", "UniversityOfFreiburg"), "test");
        byte[] data = new byte[MergeConst.CHUNK_SIZE];
        Arrays.fill(data, (byte) 64);
        provider.serveChunk("/UniversityOfFreiburg/test", data, 0, 2);
        provider.serveChunk("/UniversityOfFreiburg/test", data, 1, 2);
        File file = retriever.getFile();
        Assert.assertTrue(file != null);
        Assert.assertTrue(file.exists());
        Assert.assertTrue(!file.isDirectory());
        Assert.assertTrue(file.length() == 2 * MergeConst.CHUNK_SIZE);
    }


//    @Test
//    public void testWearFileWithProvider() throws InterruptedException {
//        WearDataRetriever retriever = new WearDataRetriever(c, ownId, "test2");
//        final WearDataProvider provider = new WearDataProvider(c);
//        provider.serve("test2", testFile);
//        File file = retriever.getFile(); // the latch should automatically release
//        Assert.assertTrue(file != null);
//        Assert.assertTrue(file.exists());
//        Assert.assertTrue(!file.isDirectory());
//        Assert.assertTrue(file.length() == testFile.length());
//    }

    @Test
    public void testWearFileWithProviderAsync() throws InterruptedException {
        WearDataRetriever retriever = new WearDataRetriever(c, new Node("platform", ownId), "test3");
        final WearDataProvider provider = new WearDataProvider(c);

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {

                } finally {
                    provider.serve("test3", testFile);
                }
            }
        }.start();

        File file = retriever.getFile(); // the latch should block
        Assert.assertTrue(file != null);
        Assert.assertTrue(file.exists());
        Assert.assertTrue(!file.isDirectory());
        Assert.assertTrue(file.length() == testFile.length());
    }


    @Test
    public void testLocalRetriever() throws InterruptedException {

        Node thisNode = new Node("blah", Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ANDROID_ID));

        LocalDataRetriever retriever = new LocalDataRetriever(c, thisNode, "testLocal");


        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                } finally {
                    Intent intent = new Intent(RecorderStatus.FINISH_ACTION);
                    intent.putExtra(RecorderStatus.RECORDING_UUID, "testLocal");
                    intent.putExtra(RecorderStatus.FINISH_PATH, testFile.getAbsolutePath());
                    c.sendBroadcast(intent);
                }
            }
        }.start();

        File file = retriever.getFile();
        Assert.assertTrue(file != null);
        Assert.assertTrue(file.exists());
        Assert.assertTrue(!file.isDirectory());
        Assert.assertTrue(file.length() == testFile.length());
    }

    @Test
    public void testFileJoin() throws IOException {
        File destination = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test");
        IOUtils.joinFiles(destination, new File[]{testFile, testFile2});
        Assert.assertTrue(destination.exists());
        Assert.assertTrue(!destination.isDirectory());
        Assert.assertTrue(destination.length() == testFile.length() + testFile2.length());
    }

    @Test
    public void testMergeSession() throws IOException, InterruptedException {
        File destination = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/session.merged.mkv");

        ArrayList<Node> nodes = new ArrayList<>();
        Node alpha = new Node("testPlatform", "alpha");
        Node beta = new Node("testPlatform", "beta");

        alpha.setConnectionTechnologies(new ConnectionTechnology[]{new ConnectionTechnology(ConnectionTechnology.Type.LOCAL)});
        beta.setConnectionTechnologies(new ConnectionTechnology[]{new ConnectionTechnology(ConnectionTechnology.Type.LOCAL)});
        nodes.add(alpha);
        nodes.add(beta);

        Looper.prepare();
        MergeSession session = new MergeSession(c, "session", nodes);

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                } finally {
                    Intent intent = new Intent(RecorderStatus.FINISH_ACTION);
                    intent.putExtra(RecorderStatus.RECORDING_UUID, "session");
                    intent.putExtra(RecorderStatus.FINISH_PATH, testFile.getAbsolutePath());
                    c.sendBroadcast(intent);
                }
            }
        }.start();

        Thread.sleep(5000);
        // merge wont work as input is not valid mkv
        Assert.assertTrue(session.isFinished());
        Assert.assertTrue(session.isThreadPoolFinished());

    }

    private void assertFile(DataRetriever retriever) throws InterruptedException {
        File file = retriever.getFile();
        Assert.assertTrue(file != null);
        Assert.assertTrue(file.exists());
        Assert.assertTrue(!file.isDirectory());
        Assert.assertTrue(file.length() == testFile.length());
    }


    private File createTestFile() {
        Random random = new Random();

        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test" + random.nextInt());
        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(f));
            for (int i = 0; i <= 100000; i++)
                fileWriter.write(random.nextInt());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }


    @After
    public void destroy() {
//         new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/test_test.part0").delete();
//         new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/test_test.part1").delete();
//         new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/test_test.merged").delete();
        mGoogleApiClient.disconnect();
        testFile.delete();
        testFile2.delete();
        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test").delete();
    }

}
