package com.edukit.launcher;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class EduBridge {
    private final Activity activity;
    private final Runnable onTimeUpdated;

    public EduBridge(Activity activity, Runnable onTimeUpdated) {
        this.activity = activity;
        this.onTimeUpdated = onTimeUpdated;
    }

    @JavascriptInterface
    public void rewardSeconds(int secondsToAdd) {
        if (secondsToAdd <= 0) return;

        int current = ConfigManager.getScreenTime(activity);
        int updated = current + secondsToAdd;
        ConfigManager.updateScreenTime(activity, updated);

        activity.runOnUiThread(() -> {
            if (onTimeUpdated != null) {
                onTimeUpdated.run();
            }
            Toast.makeText(activity, "+" + secondsToAdd + "s earned!", Toast.LENGTH_SHORT).show();
        });
    }
}
