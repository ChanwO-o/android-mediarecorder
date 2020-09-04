package com.parkchanwoo.videorecordingdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "Log_MainActivity";
	private static final int REQUEST_CODE = 1000;
	private int mScreenDensity;
	private MediaProjectionManager mProjectionManager;
	private static final int DISPLAY_WIDTH = 720;
	private static final int DISPLAY_HEIGHT = 1280;
	private MediaProjection mMediaProjection;
	private VirtualDisplay mVirtualDisplay;
	private MediaProjectionCallback mMediaProjectionCallback;
	private ToggleButton mToggleButton;
	private MediaRecorder mMediaRecorder;
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
	private static final int REQUEST_PERMISSIONS = 10;

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

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mScreenDensity = metrics.densityDpi;
		mMediaRecorder = new MediaRecorder();
		mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		mToggleButton = findViewById(R.id.toggle);
		mToggleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
					Log.e(TAG, "permissions not granted yet;");
					if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
						Log.e(TAG, "showing permission rationale");
						mToggleButton.setChecked(false);
						Snackbar.make(findViewById(android.R.id.content), R.string.label_permissions, Snackbar.LENGTH_INDEFINITE)
								.setAction("ENABLE", new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
									}
								}).show();
						Log.e(TAG, "showing snackbar");
					}
					else {
						Log.e(TAG, "requesting permissions");
						ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
					}
				}
				else {
					Log.e(TAG, "all permissions granted; toggling screen record");

					onToggleScreenShare(v);
				}
				*/

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
				DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
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
			mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
			mMediaRecorder.setVideoFrameRate(30);

//			OutputStream fos;
//			Uri videoUri;
//
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//				ContentResolver resolver = getContentResolver();
//				ContentValues contentValues = new ContentValues();
//				contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, "video.mp4"); // set file name
//				contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
//				contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);
//
//				videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
//				fos = resolver.openOutputStream(Objects.requireNonNull(videoUri));
//			}
//			else { // below Android Q
//				String videoDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
//				File video = new File(videoDir, "video.mp4");
//				videoUri = Uri.fromFile(video);
//				fos = new FileOutputStream(video);
//			}
//			mMediaRecorder.setOutputFile(videoUri.getPath());

//
//			if (fos != null)
//				fos.close();

//			File videoFile = new File(Environment.getExternalStorageDirectory() + "/video.mp4");
//			if (!videoFile.exists()) {
//				videoFile.mkdirs();
//				videoFile.createNewFile();
//			}
			Log.d(TAG, "initRecorder 1 setOutputFile()");
			mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/video.mp4");
			Log.d(TAG, "initRecorder 2 setOutputFile() done");

			mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			int orientation = ORIENTATIONS.get(rotation + 90);
			Log.d(TAG, "initRecorder 3");
			mMediaRecorder.setOrientationHint(orientation);
			Log.d(TAG, "initRecorder 4 calling prepare()");
			mMediaRecorder.prepare();
			Log.d(TAG, "initRecorder 5 prepare() done");
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