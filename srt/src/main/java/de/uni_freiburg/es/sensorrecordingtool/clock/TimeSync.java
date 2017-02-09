package de.uni_freiburg.es.sensorrecordingtool.clock;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.apache.commons.net.ntp.TimeInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeSync {

    NtpClient client = new NtpClient();

    private AtomicBoolean mIsDriftCalculated = new AtomicBoolean(false);
    private static TimeSync sInstance = null;
    private Long mDrift = 0L;


    /**
     * NTP Host, Android Default.
     */
    private final String HOST = "pool.ntp.org";

    /**
     * Timeout for each attempt. Android Default.
     */
    private static final int TIMEOUT = 20000;

    /**
     * Maximum number of attempts before we give up.
     */
    private final int MAX_TRIES = 3;

    private CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private Context mContext;

    private TimeSync(Context context) {
        this.mContext = context;
    }

    public static TimeSync getInstance(Context context) {
        if (sInstance == null)
            sInstance = new TimeSync(context);
        return sInstance;
    }

    private void asyncSync() {
        new AsyncTask<NtpClient, Void, Void>() {

            @Override
            protected Void doInBackground(NtpClient... clients) {
                for (int i = 0; i < MAX_TRIES; i++) {
                    if(isNetworkAvailable()) {
                        TimeInfo info = clients[0].requestTime(HOST, TIMEOUT);
                        if (info != null) {
                            mDrift = info.getOffset();
                            mIsDriftCalculated.set(true);
                            break; // We have a time!
                        }
                    } else {
                        mDrift = 0L;
                        mIsDriftCalculated.set(false);
                    }
                }

                mCountDownLatch.countDown();
                return null;
            }

        }.execute(client);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * This method calculates the clock drift compares to a NTP server. Will block if no drift has
     * been calculated or immediately return the last known value if already calculated.
     *
     * @return clock drift in ms or 0 if invalid; Please refer to isDriftCalculated()
     * @throws InterruptedException if the Thread is interrupted, the computation is invalid.
     */
    public long getDrift() throws InterruptedException {
        if (mIsDriftCalculated.get())
            return mDrift;
        else {
            mCountDownLatch = new CountDownLatch(1);
            asyncSync();
            mCountDownLatch.await();
            return mDrift;
        }

    }


    /**
     * @return
     * @throws InterruptedException
     */
    public long getCorrectTime() throws InterruptedException {
        return System.currentTimeMillis() + getDrift();
    }

    /**
     * @return True if a Clock-Drift has been successfully determined.
     */
    public boolean isDriftCalculated() {
        return mIsDriftCalculated.get();
    }

}
