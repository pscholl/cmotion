package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.SensorEventListener;
import android.opengl.GLES10Ext;
import android.opengl.GLES11Ext;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import de.uni_freiburg.es.sensorrecordingtool.PermissionDialog;
import de.uni_freiburg.es.sensorrecordingtool.sensors.Sensor;

/** Grabs frames at the specified videorate and returns them in raw format at maximum
 * resolution as sensorevents.
 *
 * Created by phil on 4/26/16.
 */
public class VideoSensor extends Sensor implements SurfaceHolder.Callback {
    protected static final String TAG = VideoSensor.class.getName();
    public static final String SENSOR_NAME = "video";
    protected final Context context;
    private Camera mCamera;
    private int mRateInMilliHz = 0;
    private SurfaceView mSurface;

    public VideoSensor(Context c) {
        super(c, 1);
        context = c;
    }

    @Override
    public String getStringType() {
        return SENSOR_NAME;
    }

    @Override
    public void registerListener(SensorEventListener l, int rate_in_mus, int delay) {
        if (!PermissionDialog.camera(context))
            return;

        if (mListeners.size() == 0) {
            mRateInMilliHz = (int) (1000 * 1000 / rate_in_mus) * 1000;
            startRecording();
        }

        super.registerListener(l,rate_in_mus,delay);
    }

    /** can't do a recording without a preview surface, which is why a system overlay is created
     * here that can seen from anywhere.
     */
    public void startRecording() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mSurface = new SurfaceView(mContext);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            8, 8, 0, 0,
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.LEFT  | Gravity.TOP;
        wm.addView(mSurface, lp);
        mSurface.getHolder().addCallback(this);
    }


    protected Camera.PreviewCallback preview = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            //mEvent.values = bytes;
        Log.d(TAG, "preview called " + bytes.length);
        }
    };


    @Override
    public void unregisterListener(SensorEventListener l) {
        super.unregisterListener(l);

        if (mListeners.size() == 0)
            ;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Camera.Parameters p = null;

        try {
            mCamera = Camera.open();
            p = mCamera.getParameters();
            p.setPreviewFpsRange(mRateInMilliHz, mRateInMilliHz);
            mCamera.setParameters(p);

            mCamera.setPreviewDisplay(mSurface.getHolder());
            mCamera.setPreviewCallback(preview);
            mCamera.startPreview();

        } catch(Exception e) {
            if (mCamera == null) {
                Log.d(TAG, "unable to get camera object");
                mCamera.stopPreview();
                mCamera.release();
            } else {
                Log.d(TAG, e.toString());
                Log.d(TAG, "available rates are:");

                if (p != null)
                    for (int r[] : p.getSupportedPreviewFpsRange())
                        Log.d(TAG, Integer.toString(r[0]) + " " + Integer.toString(r[1]) + " milli-Hz");
            }

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
