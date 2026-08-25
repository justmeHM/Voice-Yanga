package com.voiceyanga.citizen.core.utils;

import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Utility for providing enterprise-level tactile feedback.
 */
public class HapticHelper {

    public static void performClick(View view) {
        if (view != null) {
            view.setHapticFeedbackEnabled(true);
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    public static void performLongPress(View view) {
        if (view != null) {
            view.setHapticFeedbackEnabled(true);
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    public static void performSuccess(View view) {
        if (view != null) {
            view.setHapticFeedbackEnabled(true);
            // On some devices VIRTUAL_KEY is silent, try to use a more standard one
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }
}
