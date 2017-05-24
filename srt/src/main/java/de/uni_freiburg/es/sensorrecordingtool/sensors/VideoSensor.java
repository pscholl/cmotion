package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

/**
 * Grabs frames at the specified videorate and returns them in raw format at maximum
 * resolution as sensorevents.
 * <p>
 * Created by phil on 4/26/16.
 */
public class VideoSensor extends Sensor implements Camera.ErrorCallback {
    protected static final String TAG = VideoSensor.class.getName();
    protected final Context context;
    private final int id;
    private int facing;
    private Camera mCamera;
    private int mRateInMilliHz = 0;
    private SurfaceTexture mSurfaceTexture;
    private Camera.CameraInfo info;

    public VideoSensor(Context c, int id) {
        super(c, 1);
        context = c;
        this.id = id;

        info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        this.facing = info.facing;
        mEvent = new SensorEvent(0);
    }

    @Override
    public String getStringName() {
        return String.format("Video %s", facing == Camera.CameraInfo.CAMERA_FACING_BACK ? "back" : "front");
    }

    @Override
    public String getStringType() {
        return String.format("android.hardware.video_%s", facing == Camera.CameraInfo.CAMERA_FACING_BACK ? "back" : "front");
    }

    @Override
    public void registerListener(SensorEventListener l, double rate, String format, Handler h) {
        //if (!PermissionDialog.camera(context))
        //    return;

        if (mListeners.size() == 0) {

            mRateInMilliHz = (int) (1e3 / rate);

            /** open the camera if we are just creating the first listeners, otherwise just
             * add a new listener. */
            CameraSize mSize = getCameraSize(format);
            newOpenCamera();
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(mSize.width, mSize.height);
            params.setPreviewFpsRange(mRateInMilliHz, mRateInMilliHz);
            Log.d(TAG, "starting recording with pixel format " + params.getPictureFormat());
            Log.d(TAG, "resolution " + params.getPreviewSize().width + "x" + params.getPreviewSize().height);

            params.setRotation(getCorrectCameraOrientation(info, mCamera));
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(getCorrectCameraOrientation(info, mCamera));

            int bytesPerBuffer = (int) Math.ceil(
                    ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8.
                            * mSize.width * mSize.height);

//            mCamera.addCallbackBuffer(new byte[bytesPerBuffer]);
//            mCamera.addCallbackBuffer(new byte[bytesPerBuffer]);

            startRecording();
        }

        super.registerListener(l, rate, format, h);
    }

    @Override


    public void unregisterListener(SensorEventListener l) {
        super.unregisterListener(l);

        if (mListeners.size() == 0) {
            stopRecording();
        }
    }

    @Override
    public int getFifoSize() {
        return 0;
    }


    private void oldOpenCamera() {
        try {
            mCamera = Camera.open();
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to open front camera");
        }
    }

    /**
     * Determine whethe the code is running on Google Glass
     *
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }

    public static CameraSize getCameraSize(String format) {
        CameraSize mSize = null;

        if (isRunningOnGlass()) { // camera impl is flawed
            return new CameraSize(320, 240);
        }

        try {
            Camera cam = Camera.open();
            Camera.Parameters params = cam.getParameters();
            Camera.Size tmpSize = params.getPreviewSize();

            mSize = new CameraSize(tmpSize.width, tmpSize.height);

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
                    (format != null ? format : ""), mSize == null ? 0 : mSize.width, mSize == null ? 0 : mSize.height));
        }

        return mSize;
    }


    /**
     * can't do a recording without a preview surface, which is why a system overlay is created
     * here that it can seen from anywhere.
     */
    @Override
    public void prepareSensor() {

        setPrepared();
    }

    @Override
    public void startRecording() {

        try {

            Camera.Parameters params = mCamera.getParameters();

            mSurfaceTexture = new SurfaceTexture(10);
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.setPreviewCallback(preview);
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


    public int getCorrectCameraOrientation(Camera.CameraInfo info, Camera camera) {

//        int rotation = context..getWindowManager().getDefaultDisplay().getRotation();
        int rotation = 180;
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    private int getBackFacingCameraId() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {

                cameraId = i;
                break;
            }
        }
        return cameraId;
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

            mEvent.timestamp = System.currentTimeMillis() * 1000 * 1000;
            mEvent.rawdata = bytes;
            notifyListeners();

//            try {
//                /** add the buffer again to the queue */
//                mCamera.addCallbackBuffer(bytes);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
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
