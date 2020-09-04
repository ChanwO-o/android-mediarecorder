package com.parkchanwoo.videorecordingdemo;

import android.app.Activity;
import android.util.DisplayMetrics;

class DisplayUtils {

	public static int getScreenWidthPixels(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics.widthPixels;
	}

	public static int getScreenHeightPixels(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics.heightPixels;
	}

	public static int getScreenDensity(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics.densityDpi;
	}
}
