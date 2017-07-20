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
import android.view.WindowManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    private CameraSize mSize;
    private boolean mRecord = false;

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
            mRecord = true;
        }

        super.registerListener(l, rate, format, h);
    }

    /**
     * can't do a recording without a preview surface, which is why a system overlay is created
     * here that it can seen from anywhere.
     */
    @Override
    public void prepareSensor(double rate, String format) {

        mRateInMilliHz = (int) (rate * 1e3);

        /** open the camera if we are just creating the first listeners, otherwise just
         * add a new listener. */
        mSize = getCameraSize(format, id);
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

        startRecording();
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
            mCamera = Camera.open(id);
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

    public static CameraSize getCameraSize(String format, int camerId) {
        CameraSize size = null;

        if (isRunningOnGlass() || true) { // camera impl is flawed
            return new CameraSize(320, 240);
        }

        try {
            Camera cam = Camera.open(camerId);
            Camera.Parameters params = cam.getParameters();
            Camera.Size tmpSize = params.getPreviewSize();

            size = new CameraSize(tmpSize.width, tmpSize.height);

            cam.unlock();
            cam.release();

            if (format != null && format.contains("x")) {

                String[] wh = format.split("x");
                int w = Integer.parseInt(wh[0]),
                        h = Integer.parseInt(wh[1]);

                size.width = w;
                size.height = h;
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "camera not available");
        } catch (Exception e) {
            Log.d(TAG, String.format(
                    "unable to parse format '%s', using default resolution %dx%d",
                    (format != null ? format : ""), size == null ? 0 : size.width, size == null ? 0 : size.height));
        }

        return size;
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

        int rotation = ((WindowManager) context.getApplicationContext().
                getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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

        Log.w(TAG, "Rotate by "+result);

        return result;
    }

//    private int getBackFacingCameraId() {
//        int cameraId = -1;
//        // Search for the front facing camera
//        int numberOfCameras = Camera.getNumberOfCameras();
//        for (int i = 0; i < numberOfCameras; i++) {
//            Camera.CameraInfo info = new Camera.CameraInfo();
//            Camera.getCameraInfo(i, info);
//            if (info.facing == this.id) {
//
//                cameraId = i;
//                break;
//            }
//        }
//        return cameraId;
//    }

    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, "ERROR: " + error);
    }

    public int getCameraID() {
        return id;
    }

    public int getCameraRotation() {
        Camera cam = Camera.open(id);
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        cam.unlock();
        cam.release();

        return getCorrectCameraOrientation(info, cam);
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

    Executor excecutor = Executors.newFixedThreadPool(1);

    protected Camera.PreviewCallback preview = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] bytes, Camera camera) {
//            Log.e(TAG, "frame #" + frames++);

            if(!isPrepared())
                setPrepared();

            if(!mRecord)
                return;

            final long time = System.currentTimeMillis() * 1000 * 1000;
            if (bytes != null)
                excecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mEvent.timestamp = time;
                        mEvent.rawdata = bytes;
                        notifyListeners();
                        mEvent.rawdata = null;
                    }
                });

        }

        // Default format is YCbCr'NV21
    };

    public byte[] rotateNV21(byte[] input, int width, int height, int rotation) {
        byte[] output = new byte[input.length];
        boolean swap = (rotation == 90 || rotation == 270);
        boolean yflip = (rotation == 90 || rotation == 180);
        boolean xflip = (rotation == 270 || rotation == 180);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xo = x, yo = y;
                int w = width, h = height;
                int xi = xo, yi = yo;
                if (swap) {
                    xi = w * yo / h;
                    yi = h * xo / w;
                }
                if (yflip) {
                    yi = h - yi - 1;
                }
                if (xflip) {
                    xi = w - xi - 1;
                }
                output[w * yo + xo] = input[w * yi + xi];
                int fs = w * h;
                int qs = (fs >> 2);
                xi = (xi >> 1);
                yi = (yi >> 1);
                xo = (xo >> 1);
                yo = (yo >> 1);
                w = (w >> 1);
                h = (h >> 1);
                // adjust for interleave here
                int ui = fs + (w * yi + xi) * 2;
                int uo = fs + (w * yo + xo) * 2;
                // and here
                int vi = ui + 1;
                int vo = uo + 1;
                output[uo] = input[ui];
                output[vo] = input[vi];
            }
        }
        return output;
    }

    public byte[] toPrimitve(Byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++)
            out[i] = in[i];
        return out;
    }

    public Byte[] toComplex(byte[] in) {
        Byte[] out = new Byte[in.length];
        for (int i = 0; i < in.length; i++)
            out[i] = in[i];
        return out;
    }

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
