package com.voiceyanga.citizen.core.utils;

import android.app.Activity;
import android.util.Log;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * Utility helper to handle Google Play In-App Updates and In-App Review prompts.
 */
public class PlayServicesHelper {

    private static final String TAG = "PlayServicesHelper";
    public static final int UPDATE_REQUEST_CODE = 9001;

    public static void checkForAppUpdates(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        try {
            AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    try {
                        appUpdateManager.startUpdateFlow(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start update flow", e);
                    }
                }
            }).addOnFailureListener(e -> Log.d(TAG, "App update check unavailable or unneeded"));
        } catch (Throwable t) {
            Log.e(TAG, "Play AppUpdateManager initialization failed", t);
        }
    }

    public static void requestInAppReview(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        try {
            ReviewManager reviewManager = ReviewManagerFactory.create(activity);
            reviewManager.requestReviewFlow().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    ReviewInfo reviewInfo = task.getResult();
                    reviewManager.launchReviewFlow(activity, reviewInfo);
                } else {
                    Log.d(TAG, "Review flow request completed without result");
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Play ReviewManager initialization failed", t);
        }
    }
}
