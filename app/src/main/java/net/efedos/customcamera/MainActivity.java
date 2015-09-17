package net.efedos.customcamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.efedos.customcamera.camera.CameraPreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends Activity {
    private static appPhase PHASE;
    private Camera maCamera;
    private FrameLayout maLayoutPreview;
    private CameraPreview maPreview;
    private String TAG = "MainActivity3Activity";


    private static boolean TOP_MENU_OPENED = false;
    //ImageView image;
    Activity _this;
    String path = "/sdcard/huggies_picframe/cache/images/";
    private Bitmap resizedbitmap;
    private String fileUrl = "";
    private int base_width = 480;
    private int base_height = 480;

    public enum appPhase {
        CAMERA_PREVIEW,
        PHOTO_PREVIEW,
        FRAMED_PHOTO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);


        ////////////
        _this =this;
        maLayoutPreview = (FrameLayout) findViewById(R.id.fragment_preview);
        ////////////

        PHASE = appPhase.CAMERA_PREVIEW;

        initListeners();
    }

    private void initCameraPreview() {
        if ( PHASE.equals(appPhase.CAMERA_PREVIEW) ) {
            fileUrl = "";
            //image.setVisibility(View.INVISIBLE);
            if (maCamera == null) maCamera = openFrontFacingCameraGingerbread();
            else {
                try {
                    maCamera.startPreview();
                } catch (Exception ignored) {

                }
            }
            if (maPreview == null) {
                maPreview = new CameraPreview(_this, maCamera);
                //maPreview.setKeepScreenOn(true);
                if (maCamera != null) {
                    if (Build.VERSION.SDK_INT >= 14)
                        setCameraDisplayOrientation(_this, Camera.CameraInfo.CAMERA_FACING_BACK, maCamera);
                }

                Point displayDim = getDisplayWH();
                Point layoutPreviewDim = calcCamPrevDimensions(displayDim,
                        maPreview.getOptimalPreviewSize(maPreview.prSupportedPreviewSizes, displayDim.x, displayDim.x));
                if (layoutPreviewDim != null) {
                    RelativeLayout.LayoutParams layoutPreviewParams =
                            (RelativeLayout.LayoutParams) maLayoutPreview.getLayoutParams();
                    layoutPreviewParams.width = layoutPreviewDim.x;
                    layoutPreviewParams.height = layoutPreviewDim.y;
                    layoutPreviewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                    maLayoutPreview.setLayoutParams(layoutPreviewParams);
                }
                maLayoutPreview.addView(maPreview);
            }
        }
    }

    private void stopCameraPreview(){
        if (maCamera != null){
            maCamera.setPreviewCallback(null);
            maCamera.stopPreview();
            maCamera.release();        // release the camera for other applications
            maCamera = null;
            maPreview.getHolder().removeCallback(maPreview);
            maPreview = null;
        }
        maLayoutPreview.removeAllViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        base_width = size.x;
        base_height = size.x;
        initCameraPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCameraPreview();              // release the camera immediately on pause event
    }

    @Override
    protected void onStop(){
        super.onStop();
        stopCameraPreview();              // release the camera immediately on pause event
    }

    private void initListeners() {
        /*fotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PHASE = appPhase.PHOTO_PREVIEW;
                if(TOP_MENU_OPENED) closeTopMenu();
                try {
                    takeFocusedPicture();
                } catch (Exception e) {

                }
                //exitButton.setClickable(false);
                fotoButton.setClickable(false);
                progressLayout.setVisibility(View.VISIBLE);
            }
        });

        btnReintentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PHASE = appPhase.CAMERA_PREVIEW;
                initCameraPreview();
            }
        });*/
    }


    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void takeFocusedPicture() {
        //fotoButton.setVisibility(View.INVISIBLE);
        //btnReintentar.setVisibility(View.VISIBLE);
        maCamera.autoFocus(mAutoFocusCallback);
    }

    Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            try{
                camera.takePicture(mShutterCallback, null, jpegCallback);
            }catch(Exception e){
                e.getStackTrace();
            }
        }
    };

    Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {

        @Override
        public void onShutter() {

        }
    };
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @SuppressWarnings("deprecation")
        public void onPictureTaken(byte[] data, Camera camera) {

            FileOutputStream outStream = null;
            Calendar c = Calendar.getInstance();
            File videoDirectory = new File(path);

            if (!videoDirectory.exists()) {
                videoDirectory.mkdirs();
            }

            try {
                // Write to SD Card
                outStream = new FileOutputStream(path + c.getTime().getSeconds() + ".jpg");
                outStream.write(data);
                outStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 6;
            //options.inJustDecodeBounds = true;
            options.inDither=false;                     //Disable Dithering mode
            options.inPurgeable=true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
            options.inInputShareable=true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future

            Bitmap realImage;
            realImage = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            /*try{
                Log.i(TAG,"realImage.width: "+realImage.getWidth());
            }catch (NullPointerException e){
                e.getStackTrace();
            }*/
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(path + c.getTime().getSeconds() + ".jpg");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
                        .equalsIgnoreCase("1")) {
                    realImage = rotate(realImage, 90);
                } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
                        .equalsIgnoreCase("8")) {
                    realImage = rotate(realImage, 90);
                } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
                        .equalsIgnoreCase("3")) {
                    realImage = rotate(realImage, 90);
                } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
                        .equalsIgnoreCase("0")) {
                    realImage = rotate(realImage, 90);
                }
            } catch (Exception e) {

            }

//            int width=(int)(realImage.getWidth()*90/100);
//            int height=realImage.getHeight();

            //pausePreview();

            //Gz.d("realImage.getWidth(): "+realImage.getWidth());
            resizedbitmap= Bitmap.createBitmap(realImage, 0, 0, realImage.getWidth(), realImage.getWidth());
            //realImage.recycle();

            PHASE = appPhase.PHOTO_PREVIEW;
            //marco.setVisibility(View.INVISIBLE);
            //camera.startPreview();
            //exitButton.setClickable(true);

        }
    };

    public static Bitmap rotate(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        //Gz.d("MainActivity.rotate() source.getWidth(): "+source.getWidth());
        Bitmap bm = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
        //return flip(bm);
        return bm;
    }


    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
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
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        maPreview.setCamera(camera);
    }

    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private Point getDisplayWH() {

        Display display = this.getWindowManager().getDefaultDisplay();
        Point displayWH = new Point();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(displayWH);
            return displayWH;
        }
        displayWH.set(display.getWidth(), display.getHeight());
        return displayWH;
    }

    private Point calcCamPrevDimensions(Point disDim, Camera.Size camDim) {

        Point displayDim = disDim;
        Camera.Size cameraDim = camDim;

        double widthRatio = (double) displayDim.x / cameraDim.width;
        double heightRatio = (double) displayDim.y / cameraDim.height;

        // use ">" to zoom preview full screen
        /*if (widthRatio < heightRatio) {
            Point calcDimensions = new Point();
            calcDimensions.x = displayDim.x;
            calcDimensions.y = (displayDim.x * cameraDim.height) / cameraDim.width;
            return calcDimensions;
        }*/
        // use "<" to zoom preview full screen
        if (widthRatio > heightRatio) {
            Point calcDimensions = new Point();
            calcDimensions.x = (displayDim.y * cameraDim.width) / cameraDim.height;
            calcDimensions.y = displayDim.y;
            return calcDimensions;
        }
        return null;
    }
}