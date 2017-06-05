package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import android.view.SurfaceView;

import org.junit.Test;

import de.uni_freiburg.es.sensorrecordingtool.sensors.VideoSensor;

/**
 * Created by paulgavrikov on 04.05.17.
 */

public class VideoSensorScratchTest implements Camera.ErrorCallback {

    protected static final String TAG = VideoSensor.class.getName();
    protected final Context context;
    private final int id = 1;
    private int facing;
    private Camera mCamera;
    private int mRateInMilliHz = 0;
    private SurfaceView mSurface;


    @Test
    public void test() throws InterruptedException {
        start();
//        while (true) yield();
        Looper.prepare();
        Thread.sleep(100000);
    }

    public VideoSensorScratchTest() {
        context = InstrumentationRegistry.getTargetContext();;

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        this.facing = info.facing;
    }


    public void start() {
            /** open the camera if we are just creating the first listeners, otherwise just
             * add a new listener. */
            newOpenCamera();
            Camera.Parameters params = mCamera.getParameters();
            Log.d(TAG, "starting recording with pixel format " + params.getPictureFormat());
            Log.d(TAG, "resolution " + params.getPreviewSize().width + "x" + params.getPreviewSize().height);
            mCamera.setParameters(params);
            startRecording();
    }




    private void oldOpenCamera() {
        try {
            mCamera = Camera.open();
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to open front camera");
        }
    }

    /**
     * Determine whethe the code is runnong on Google Glass
     *
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }

    public static VideoSensor.CameraSize getCameraSize(String format) {
        VideoSensor.CameraSize mSize = null;

        if (isRunningOnGlass()) { // camera impl is flawed
            return new VideoSensor.CameraSize(320, 240);
        }

        try {
            Camera cam = Camera.open();
            Camera.Parameters params = cam.getParameters();
            Camera.Size tmpSize = params.getPreviewSize();

            mSize = new VideoSensor.CameraSize(tmpSize.width, tmpSize.height);

            cam.unlock();
            cam.release();

            if (format != null && format.contains("x")) {

                String[] wh = format.split("x");
                int w = Integer.parseInt(wh[0]),
                        h = Integer.parseInt(wh[1]);

                mSize.width = w;
                mSize.height = h;
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "camera not available");
        } catch (Exception e) {
            Log.d(TAG, String.format(
                    "unable to parse format '%s', using default resolution %dx%d",
                    (format != null ? format : ""), mSize.width, mSize.height));
        }
        return mSize;
    }



    public void startRecording() {

        try {
            mCamera.setPreviewTexture(new SurfaceTexture(10));
            mCamera.setPreviewCallbackWithBuffer(preview);
            mCamera.setErrorCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {
            stopRecording();
        }


    }

    public void stopRecording() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, "ERROR: " + error);
    }


    public static class CameraSize {
        public int width, height;

        public CameraSize(int w, int h) {
            width = w;
            height = h;
        }
    }

    private void newOpenCamera() {
        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }
    }


    int frames = 0;

    protected Camera.PreviewCallback preview = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {

            Log.e(TAG, "frame #" + frames++);

            try {
                /** add the buffer again to the queue */
                mCamera.addCallbackBuffer(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Default format is YCbCr'NV21
    };

    private CameraHandlerThread mThread = null;

    private class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    oldOpenCamera();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }

    }
}
