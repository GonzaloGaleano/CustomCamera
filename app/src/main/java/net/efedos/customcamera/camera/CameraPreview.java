package net.efedos.customcamera.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by gonzalo on 31/03/2015.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreview";
    private SurfaceHolder prHolder;
    private Camera prCamera;
    public List<Camera.Size> prSupportedPreviewSizes;
    private Camera.Size prPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        prCamera = camera;

        prSupportedPreviewSizes = prCamera.getParameters().getSupportedPreviewSizes();

        prHolder = getHolder();
        prHolder.addCallback(this);
        prHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /*public void setCenterPosition(int x, int y) {
        mCenterPosX = x;
        mCenterPosY = y;
    }*/

    public void setCamera(Camera camera) {
        prCamera = camera;
        if (prCamera != null) {
            mSupportedPreviewSizes = prCamera.getParameters().getSupportedPictureSizes();
            requestLayout();

            // get Camera parameters
            Camera.Parameters params = prCamera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
                prCamera.setParameters(params);
            }

        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            prCamera.setPreviewDisplay(holder);
            prCamera.startPreview();
        } catch (IOException e) {
            Log.d("Yologram", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        prCamera.stopPreview();
        getHolder().removeCallback(this);
        prCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (prHolder.getSurface() == null){
            return;
        }

        try {
            prCamera.stopPreview();
        } catch (Exception e){
        }

        try {
            Camera.Parameters parameters = prCamera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            Log.i(TAG, "prPreviewSize.width: " + prPreviewSize.width + ", prPreviewSize.height: " + prPreviewSize.height);
            parameters.setPreviewSize(prPreviewSize.width, prPreviewSize.height);

            prCamera.setParameters(parameters);
            prCamera.setPreviewDisplay(prHolder);
            prCamera.startPreview();

        } catch (Exception e){
            Log.d("Yologram", "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);

        if (prSupportedPreviewSizes != null) {
            prPreviewSize =
                    getOptimalPreviewSize(prSupportedPreviewSizes, width, height);
        }
    }

    public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }
}