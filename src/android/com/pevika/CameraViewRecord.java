package com.pevika;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class CameraViewRecord extends CordovaPlugin implements CameraActivity.CameraPreviewListener {

	private final String TAG = "CameraViewRecord";
	private final String setOnVideoTakenHandlerAction = "setOnVideoTakenHandler";
	private final String startCameraAction = "startCamera";
	private final String stopCameraAction = "stopCamera";
	private final String showCameraAction = "showCamera";
	private final String hideCameraAction = "hideCamera";
	private final String startRecordingAction = "startRecording";
	private final String stopRecordingAction = "stopRecording";

	private CameraActivity fragment;
	private CallbackContext takePictureCallbackContext;
	private CallbackContext takeVideoCallbackContext;
	private int containerViewId = 1;
	public CameraViewRecord(){
		super();
		Log.d(TAG, "Constructing");
	}

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, ">>> " + action);
		boolean ret = false;
        if (startCameraAction.equals(action)){
    		ret = startCamera(args, callbackContext);
    	}
	    else if (stopCameraAction.equals(action)){
		    ret = stopCamera(args, callbackContext);
	    }
	    else if (hideCameraAction.equals(action)){
		    ret = hideCamera(args, callbackContext);
	    }
	    else if (showCameraAction.equals(action)){
		    ret = showCamera(args, callbackContext);
	    }
		else if (startRecordingAction.equals(action)) {
			ret = startRecording(args, callbackContext);
		}
		else if (stopRecordingAction.equals(action)) {
			ret = stopRecording(args, callbackContext);
		}
		else if (setOnVideoTakenHandlerAction.equals(action)) {
			ret = setOnVideoTakenHandler(args, callbackContext);
		}

    	return ret;
    }

	private boolean startCamera(final JSONArray args, CallbackContext callbackContext) {
        if(fragment != null){
	        return false;
        }
		fragment = new CameraActivity();
		fragment.setEventListener(this);

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {

				try {
					DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
					int x = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(0), metrics);
					int y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(1), metrics);
					int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(2), metrics);
					int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(3), metrics);
					String defaultCamera = "front";

					fragment.defaultCamera = defaultCamera;
					fragment.setRect(x, y, width, height);

					//create or update the layout params for the container view
					FrameLayout containerView = (FrameLayout) cordova.getActivity().findViewById(containerViewId);
					if (containerView == null) {
						containerView = new FrameLayout(cordova.getActivity().getApplicationContext());
						containerView.setId(containerViewId);
						FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
						cordova.getActivity().addContentView(containerView, containerLayoutParams);
					}
					//set camera back to front
					containerView.setAlpha(1.0f);
					containerView.bringToFront();
					//add the fragment to the container
					FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
					FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
					fragmentTransaction.add(containerView.getId(), fragment);
					fragmentTransaction.commit();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		return true;
	}

	private View getView() {
		try {
			return (View) webView.getClass().getMethod("getView").invoke(webView);
		} catch (Exception e) {
			return (View) webView;
		}
	}

	public void onVideoTaken(String originalVideoPath) {
		JSONArray data = new JSONArray();
		data.put(originalVideoPath);
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
		pluginResult.setKeepCallback(true);
		takeVideoCallbackContext.sendPluginResult(pluginResult);
	}

	private boolean stopCamera(final JSONArray args, CallbackContext callbackContext) {
		Log.d(TAG, "stopCamera");
		if(fragment == null){
			return false;
		}
		fragment.removeCameraPreview();
		FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.remove(fragment);
		fragmentTransaction.commit();
		fragment = null;
		cordova.getActivity().setContentView(getView());
		return true;
	}

	private boolean showCamera(final JSONArray args, CallbackContext callbackContext) {
		Log.d(TAG, "showCamera");
		if(fragment == null){
			return false;
		}

		FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.show(fragment);
		fragmentTransaction.commit();

		return true;
	}
	private boolean hideCamera(final JSONArray args, CallbackContext callbackContext) {
		if(fragment == null) {
			return false;
		}

		FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.hide(fragment);
		fragmentTransaction.commit();

		return true;
	}
	private boolean setOnVideoTakenHandler(JSONArray args, CallbackContext callbackContext) {
		Log.d(TAG, "setOnVideoTakenHandler");
		takeVideoCallbackContext = callbackContext;
		return true;
	}

	private boolean startRecording(JSONArray args, CallbackContext callbackContext) {
		Log.d(TAG, "startRecording");
		if (fragment != null) {
			try {
				if (fragment.startRecording(args.getInt(0)) == true) {
					this.sendSuccess(callbackContext);
					return true;
				}
			} catch (Exception e) {
				Log.d(TAG, "exception");
			}
		}
		this.sendFailure(callbackContext);
		return false;
	}

	private void sendFailure(CallbackContext callbackContext) {
		PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR);
		pluginResult.setKeepCallback(true);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendSuccess(CallbackContext callbackContext) {
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
		pluginResult.setKeepCallback(true);
		callbackContext.sendPluginResult(pluginResult);
	}

	private boolean stopRecording(JSONArray args, CallbackContext callbackContext) {
		Log.d(TAG, "stopRecording");
		if (fragment == null) {
			return false;
		}
		fragment.stopRecording();
		return true;
	}
}
