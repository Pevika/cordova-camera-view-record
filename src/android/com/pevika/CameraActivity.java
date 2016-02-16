package com.pevika;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.apache.cordova.LOG;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraActivity extends Fragment implements MediaRecorder.OnInfoListener {

	public interface CameraPreviewListener {
		public void onVideoTaken(String originalVideoPath);
	}

	private CameraPreviewListener eventListener;
	private static final String TAG = "CameraActivity";
	public FrameLayout mainLayout;
	public FrameLayout frameContainerLayout;

	private MediaRecorder mediaRecorder;
	private File mFile;

    private Preview mPreview;

	private View view;
	private Camera.Parameters cameraParameters;
	private Camera mCamera;
	private int numberOfCameras;
	private int cameraCurrentlyLocked;

    // The first rear facing camera
    private int defaultCameraId;
	public String defaultCamera;
	public boolean tapToTakePicture;
	public boolean dragEnabled;

	public int width;
	public int height;
	public int x;
	public int y;

	public void setEventListener(CameraPreviewListener listener){
		eventListener = listener;
	}

	private String appResourcesPackage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    appResourcesPackage = getActivity().getPackageName();

	    // Inflate the layout for this fragment
	    view = inflater.inflate(getResources().getIdentifier("camera_activity", "layout", appResourcesPackage), container, false);
	    createCameraPreview();
	    return view;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    }
	public void setRect(int x, int y, int width, int height){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void removeCameraPreview() {
		if (mainLayout != null) {
			mainLayout.removeAllViews();
		}
	}

	private void createCameraPreview(){
        if(mPreview == null) {
            setDefaultCameraId();
	        //set box position and size
	        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
	        layoutParams.setMargins(x, y, 0, 0);
	        frameContainerLayout = (FrameLayout) view.findViewById(getResources().getIdentifier("frame_container", "id", appResourcesPackage));
	        frameContainerLayout.setLayoutParams(layoutParams);
	        //video view
	        mPreview = new Preview(getActivity());
	        mainLayout = (FrameLayout) view.findViewById(getResources().getIdentifier("video_view", "id", appResourcesPackage));
	        mainLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
	        mainLayout.addView(mPreview);
	        mainLayout.setEnabled(false);
        }
    }
	
    private void setDefaultCameraId(){
		
		// Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();
		
		int camId = defaultCamera.equals("front") ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

		// Find the ID of the default camera
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == camId) {
				defaultCameraId = camId;
				break;
			}
		}
	}
	
    @Override
    public void onResume() {
        super.onResume();

        mCamera = Camera.open(defaultCameraId);
		setCameraParameters(cameraParameters != null ? cameraParameters : mCamera.getParameters());

        cameraCurrentlyLocked = defaultCameraId;
        
        if(mPreview.mPreviewSize == null){
			mPreview.setCamera(mCamera, cameraCurrentlyLocked);
		} else {
			mPreview.switchCamera(mCamera, cameraCurrentlyLocked);
			mCamera.startPreview();
		}

	    Log.d(TAG, "cameraCurrentlyLocked:" + cameraCurrentlyLocked);

        final FrameLayout frameContainerLayout = (FrameLayout) view.findViewById(getResources().getIdentifier("frame_container", "id", appResourcesPackage));
        ViewTreeObserver viewTreeObserver = frameContainerLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    frameContainerLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    frameContainerLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    final RelativeLayout frameCamContainerLayout = (RelativeLayout) view.findViewById(getResources().getIdentifier("frame_camera_cont", "id", appResourcesPackage));

                    FrameLayout.LayoutParams camViewLayout = new FrameLayout.LayoutParams(frameContainerLayout.getWidth(), frameContainerLayout.getHeight());
                    camViewLayout.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
                    frameCamContainerLayout.setLayoutParams(camViewLayout);
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null, -1);
            mCamera.release();
            mCamera = null;
        }
    }

    public Camera getCamera() {
      return mCamera;
    }

    public void switchCamera() {
        // check for availability of multiple cameras
        if (numberOfCameras == 1) {
            //There is only one camera available
        }
		Log.d(TAG, "numberOfCameras: " + numberOfCameras);

		// OK, we have multiple cameras.
		// Release this camera -> cameraCurrentlyLocked
		if (mCamera != null) {
			mCamera.stopPreview();
			mPreview.setCamera(null, -1);
			mCamera.release();
			mCamera = null;
		}

		// Acquire the next camera and request Preview to reconfigure
		// parameters.
		mCamera = Camera.open((cameraCurrentlyLocked + 1) % numberOfCameras);

		setCameraParameters(cameraParameters != null ? cameraParameters : mCamera.getParameters());

		cameraCurrentlyLocked = (cameraCurrentlyLocked + 1) % numberOfCameras;
		mPreview.switchCamera(mCamera, cameraCurrentlyLocked);

	    Log.d(TAG, "cameraCurrentlyLocked new: " + cameraCurrentlyLocked);

		// Start the preview
		mCamera.startPreview();
    }

    public void setCameraParameters(Camera.Parameters params) {
  		cameraParameters = params;
  		if (mCamera != null && cameraParameters != null) {
			// Check what resolutions are supported by your camera
			Camera.Size chosen = CameraUtils.getMatchingResolution(cameraParameters.getSupportedPictureSizes());
			if (chosen != null) {
				Log.d(TAG, "Chosen resolution: " + chosen.width + " "  + chosen.height);
				cameraParameters.setPictureSize(chosen.width, chosen.height);
			}
			mCamera.setParameters(cameraParameters);
  		}
    }

    public boolean hasFrontCamera(){
        return getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
			mCamera.lock();
		}
	}

	private boolean prepareMediaRecorder(final int duration) {
		try {
			mFile = File.createTempFile("record", ".mp4", getActivity().getCacheDir());
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "Bad arguments before creating tempfile");
			return false;
		} catch (IOException e) {
			Log.d(TAG, "Cannot create file");
			return false;
		}
		mediaRecorder = new MediaRecorder();
		mCamera.unlock();
		mediaRecorder.setCamera(mCamera);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
		mediaRecorder.setOutputFile(mFile.getPath());
		mediaRecorder.setMaxDuration(duration * 1000); // milliseconds
		mediaRecorder.setMaxFileSize(50000000);
		mediaRecorder.setOnInfoListener(this);
		mediaRecorder.setOrientationHint(mPreview.calculateScreenOrientation(false));
		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	public void onInfo(MediaRecorder recorder, int what, int extra) {
		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
			Log.d(TAG, "Maximum duration reached");
			this.stopRecording();
		}
	}

	private boolean mRecording = false;

	public boolean startRecording(final int duration) {
		if (mRecording) {
			Log.d(TAG, "Is already recording");
			return false;
		}
		if (prepareMediaRecorder(duration) == false) {
			Log.d(TAG, "Cannot record");
			return false;
		}
		mRecording = true;
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					mediaRecorder.start();
				} catch (final Exception ex) {
					mRecording = false;
					StringWriter sw = new StringWriter();
					ex.printStackTrace(new PrintWriter(sw));
					Log.d(TAG, "Exception while recording: " + sw.toString());
				}
			}
		});
		return true;
	}

	public void stopRecording() {
		if (mRecording == false) {
			Log.d(TAG, "Is not recording");
			return ;
		}
		mediaRecorder.stop();
		releaseMediaRecorder();
		eventListener.onVideoTaken(mFile.getAbsolutePath());
		mRecording = false;
	}

    private File getOutputMediaFile(String suffix){

	    File mediaStorageDir = getActivity().getApplicationContext().getFilesDir();
	    /*if(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED_READ_ONLY) {
		    mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + getActivity().getApplicationContext().getPackageName() + "/Files");
	    }*/
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HHmm_ss").format(new Date());
        File mediaFile;
        String mImageName = "camerapreview_" + timeStamp + suffix + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    private File storeImage(Bitmap image, String suffix) {
        File pictureFile = getOutputMediaFile(suffix);
        if (pictureFile != null) {
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                image.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.close();
                return pictureFile;
            }
            catch (Exception ex) {
            }
        }
        return null;
    }

	public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}
	
    private Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}


class Preview extends RelativeLayout implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    CustomSurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;
    Camera mCamera;
    int cameraId;
    int displayOrientation;

    Preview(Context context) {
        super(context);

        mSurfaceView = new CustomSurfaceView(context);
        addView(mSurfaceView);

        requestLayout();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

	private Camera.Parameters getCameraParameters(Camera camera) {
		if (camera != null) {
			Camera.Parameters cameraParameters = camera.getParameters();
			Camera.Size chosen = CameraUtils.getMatchingResolution(cameraParameters.getSupportedPictureSizes());
			if (chosen != null) {
				Log.d(TAG, "Chosen resolution: " + chosen.width + " "  + chosen.height);
				cameraParameters.setPictureSize(chosen.width, chosen.height);
			}
			return cameraParameters;
		}
		return null;
	}

    public void setCamera(Camera camera, int cameraId) {
        mCamera = camera;
        this.cameraId = cameraId;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            setCameraDisplayOrientation();
            //mCamera.getParameters().setRotation(getDisplayOrientation());
            //requestLayout();
        }
    }

    public int getDisplayOrientation() {
    	return displayOrientation;
    }

	public int calculateScreenOrientation(final boolean flipFront) {
		Camera.CameraInfo info=new Camera.CameraInfo();
		int rotation=
				((Activity)getContext()).getWindowManager().getDefaultDisplay()
						.getRotation();
		int degrees=0;
		DisplayMetrics dm=new DisplayMetrics();
		int orientation = 0;
		Camera.getCameraInfo(cameraId, info);
		((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(dm);

		switch (rotation) {
			case Surface.ROTATION_0:
				degrees=0;
				break;
			case Surface.ROTATION_90:
				degrees=90;
				break;
			case Surface.ROTATION_180:
				degrees=180;
				break;
			case Surface.ROTATION_270:
				degrees=270;
				break;
		}

		if (flipFront && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			orientation = (info.orientation + degrees) % 360;
			orientation = (360 - orientation) % 360;
		} else {
			orientation = (info.orientation - degrees + 360) % 360;
		}

		Log.d(TAG, "screen is rotated " + degrees + "deg from natural");
		Log.d(TAG, (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back")
				+ " camera is oriented -" + info.orientation + "deg from natural");
		Log.d(TAG, "need to rotate preview " + orientation + "deg");
		return orientation;
	}

    private void setCameraDisplayOrientation() {
        displayOrientation = calculateScreenOrientation(true);
        mCamera.setDisplayOrientation(displayOrientation);
    }

    public void switchCamera(Camera camera, int cameraId) {
        setCamera(camera, cameraId);
        try {
            camera.setPreviewDisplay(mHolder);
	        Camera.Parameters parameters = getCameraParameters(camera);
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			camera.setParameters(parameters);
        }
        catch (IOException exception) {
            Log.e(TAG, exception.getMessage());
        }
        //requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            int width = r - l;
            int height = b - t;

            int previewWidth = width;
            int previewHeight = height;

            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;

                if(displayOrientation == 90 || displayOrientation == 270) {
                    previewWidth = mPreviewSize.height;
                    previewHeight = mPreviewSize.width;
                }

	            LOG.d(TAG, "previewWidth:" + previewWidth + " previewHeight:" + previewHeight);
            }

            int nW;
            int nH;
            int top;
            int left;

            float scale = 1.0f;

            // Center the child SurfaceView within the parent.
            if (width * previewHeight < height * previewWidth) {
                Log.d(TAG, "center horizontally");
                int scaledChildWidth = (int)((previewWidth * height / previewHeight) * scale);
                nW = (width + scaledChildWidth) / 2;
                nH = (int)(height * scale);
                top = 0;
                left = (width - scaledChildWidth) / 2;
            }
            else {
                Log.d(TAG, "center vertically");
                int scaledChildHeight = (int)((previewHeight * width / previewWidth) * scale);
                nW = (int)(width * scale);
                nH = (height + scaledChildHeight) / 2;
                top = (height - scaledChildHeight) / 2;
                left = 0;
            }
            child.layout(left, top, nW, nH);

            Log.d("layout", "left:" + left);
            Log.d("layout", "top:" + top);
            Log.d("layout", "right:" + nW);
            Log.d("layout", "bottom:" + nH);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mSurfaceView.setWillNotDraw(false);
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
		Camera.Size optimalSize = CameraUtils.getMatchingResolution(sizes);
        Log.d(TAG, "optimal preview size: w: " + optimalSize.width + " h: " + optimalSize.height);
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
	    if(mCamera != null) {
		    // Now that the size is known, set up the camera parameters and begin
		    // the preview.
		    Camera.Parameters parameters = getCameraParameters(mCamera);
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		    requestLayout();
		    //mCamera.setDisplayOrientation(90);
			mCamera.setParameters(parameters);
		    mCamera.startPreview();
	    }
    }

    public byte[] getFramePicture(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int format = parameters.getPreviewFormat();

        //YUV formats require conversion
        if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {
            int w = parameters.getPreviewSize().width;
            int h = parameters.getPreviewSize().height;

            // Get the YuV image
            YuvImage yuvImage = new YuvImage(data, format, w, h, null);
            // Convert YuV to Jpeg
            Rect rect = new Rect(0, 0, w, h);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(rect, 80, outputStream);
            return outputStream.toByteArray();
        }
        return data;
    }
    public void setOneShotPreviewCallback(Camera.PreviewCallback callback) {
        if(mCamera != null) {
            mCamera.setOneShotPreviewCallback(callback);
        }
    }
}
class TapGestureDetector extends GestureDetector.SimpleOnGestureListener{

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return true;
	}
}
class CustomSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    private final String TAG = "CustomSurfaceView";

    CustomSurfaceView(Context context){
        super(context);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}

class CameraUtils {

	private final String TAG = "CameraUtils";

	public static Camera.Size getMatchingResolution (List<Camera.Size> sizes) {
		Camera.Size chosen = null;
		for (Camera.Size size : sizes) { // get the closest from the ideal
			if (chosen == null || chosen.height * chosen.width < size.height * size.width) {
				chosen = size;
			}
		}
		return chosen;
	}

}