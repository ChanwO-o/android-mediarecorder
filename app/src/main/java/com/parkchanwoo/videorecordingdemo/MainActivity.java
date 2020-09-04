package com.parkchanwoo.videorecordingdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "Log_MainActivity";
	private static final int REQUEST_CODE = 1000;
	private static final int REQUEST_PERMISSIONS = 10;

	private MediaRecorder mMediaRecorder;
	private MediaProjectionManager mProjectionManager;
	private MediaProjection mMediaProjection;
	private VirtualDisplay mVirtualDisplay;
	private MediaProjectionCallback mMediaProjectionCallback;
	private ToggleButton mToggleButton;
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

	static {
		ORIENTATIONS.append(Surface.ROTATION_0, 90);
		ORIENTATIONS.append(Surface.ROTATION_90, 0);
		ORIENTATIONS.append(Surface.ROTATION_180, 270);
		ORIENTATIONS.append(Surface.ROTATION_270, 180);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mMediaRecorder = new MediaRecorder();
		mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		mToggleButton = findViewById(R.id.toggle);
		mToggleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
				if (EasyPermissions.hasPermissions(MainActivity.this, perms)) {
					// Already have permission, do the thing
					Log.i(TAG, "all permissions granted; toggling screen record");
					onToggleScreenShare(v);
				} else {
					// Do not have permissions, request them now
					EasyPermissions.requestPermissions(MainActivity.this, getString(R.string.label_permissions),
							REQUEST_PERMISSIONS, perms);
				}
			}
		});
		startService(new Intent(MainActivity.this, RecordingService.class));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQUEST_CODE) {
			Log.w(TAG, "Unknown request code: " + requestCode);
			return;
		}
		if (resultCode != RESULT_OK) {
			Toast.makeText(this,
					"Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
			mToggleButton.setChecked(false);
			return;
		}
		mMediaProjectionCallback = new MediaProjectionCallback();
		mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
		mMediaProjection.registerCallback(mMediaProjectionCallback, null);
		mVirtualDisplay = createVirtualDisplay();
		mMediaRecorder.start();
	}

	public void onToggleScreenShare(View view) {
		if (((ToggleButton) view).isChecked()) {
			initRecorder();
			shareScreen();
		} else {
			mMediaRecorder.stop();
			mMediaRecorder.reset();
			Log.i(TAG, "Stopping Recording");
			stopScreenSharing();
		}
	}

	private void shareScreen() {
		if (mMediaProjection == null) {
			startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
			return;
		}
		mVirtualDisplay = createVirtualDisplay();
		mMediaRecorder.start();
	}

	private VirtualDisplay createVirtualDisplay() {
		return mMediaProjection.createVirtualDisplay("MainActivity",
				DisplayUtils.getScreenWidthPixels(this), DisplayUtils.getScreenHeightPixels(this), DisplayUtils.getScreenDensity(this),
				DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
				mMediaRecorder.getSurface(), null /*Callbacks*/, null
				/*Handler*/);
	}

	private void initRecorder() {
		try {
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
			mMediaRecorder.setVideoSize(DisplayUtils.getScreenWidthPixels(this), DisplayUtils.getScreenHeightPixels(this));
			mMediaRecorder.setVideoFrameRate(30);
			mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/video.mp4");
			mMediaRecorder.setVideoEncodingBitRate(512 * 10000);
			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			int orientation = ORIENTATIONS.get(rotation + 90);
			mMediaRecorder.setOrientationHint(orientation);
			mMediaRecorder.prepare();
		} catch (IOException e) {
			Log.e(TAG, "initRecorder IOException!");
			e.printStackTrace();
		}
	}

	private class MediaProjectionCallback extends MediaProjection.Callback {
		@Override
		public void onStop() {
			if (mToggleButton.isChecked()) {
				mToggleButton.setChecked(false);
				mMediaRecorder.stop();
				mMediaRecorder.reset();
				Log.i(TAG, "Recording Stopped");
			}
			mMediaProjection = null;
			stopScreenSharing();
		}
	}

	private void stopScreenSharing() {
		if (mVirtualDisplay == null) {
			return;
		}
		mVirtualDisplay.release();
		//mMediaRecorder.release(); //If used: mMediaRecorder object cannot be reused again
		destroyMediaProjection();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		destroyMediaProjection();
	}

	private void destroyMediaProjection() {
		if (mMediaProjection != null) {
			mMediaProjection.unregisterCallback(mMediaProjectionCallback);
			mMediaProjection.stop();
			mMediaProjection = null;
		}
		Log.i(TAG, "MediaProjection Stopped");
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
		Log.d(TAG, "onRequestPermissionsResult() rc = " + requestCode);

		if (requestCode != REQUEST_PERMISSIONS)
			return;

		if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
			onToggleScreenShare(mToggleButton);
		} else {
			mToggleButton.setChecked(false);
			Snackbar.make(findViewById(android.R.id.content), R.string.label_permissions,
					Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
					new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent();
							intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							intent.addCategory(Intent.CATEGORY_DEFAULT);
							intent.setData(Uri.parse("package:" + getPackageName()));
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
							intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
							startActivity(intent);
						}
					}).show();
		}
	}
}