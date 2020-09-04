package com.parkchanwoo.videorecordingdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RecordingService extends Service {
	private static final String TAG = "RecordingService";
	private final int NOTIFICATION_ID = 1234;
	private final String NOTIFICATION_CHANNEL_ID = "NOTIFICATION_CHANNEL_ID";
	private final String NOTIFICATION_CHANNEL_NAME = "NOTIFICATION_CHANNEL_NAME";
	private final String NOTIFICATION_CHANNEL_DESC = "NOTIFICATION_CHANNEL_DESC";

	@Override
	public void onCreate() {
		super.onCreate();
		startInForeground();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void startInForeground() {
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_launcher_foreground)
				.setContentTitle("TEST")
				.setContentText("HELLO")
				.setTicker("TICKER")
				.setContentIntent(pendingIntent);
		Notification notification = builder.build();
		if(Build.VERSION.SDK_INT>=26) {
			NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(NOTIFICATION_CHANNEL_DESC);
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.createNotificationChannel(channel);
		}
		startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
	}
}
