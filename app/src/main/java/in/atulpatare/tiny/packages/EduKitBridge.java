package com.example.edukit;

import android.content.Context;
import android.webkit.JavascriptInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class EduKitBridge {
    private static final String CONFIG_FILE = "edukit_config.txt";
    private static final String KEY_POINTS = "screen_points";
    private static final String KEY_PIN = "parent_pin";

    private final Context context;
    private final Runnable onStateUpdated;

    public EduKitBridge(Context context, Runnable onStateUpdated) {
        this.context = context.getApplicationContext();
        this.onStateUpdated = onStateUpdated;
    }

    @JavascriptInterface
    public synchronized int addScreenPoints(int delta, String questionId) {
        if (delta <= 0) {
            return getScreenPoints();
        }
        Properties props = loadConfig();
        int current = parseIntSafe(props.getProperty(KEY_POINTS, "0"));
        int updated = current + delta;
        props.setProperty(KEY_POINTS, String.valueOf(updated));
        saveConfig(props);

        if (onStateUpdated != null) {
            onStateUpdated.run();
        }
        return updated;
    }

    public synchronized int getScreenPoints() {
        Properties props = loadConfig();
        return parseIntSafe(props.getProperty(KEY_POINTS, "0"));
    }

    public synchronized boolean spendScreenPoints(int cost) {
        if (cost <= 0) return true;
        Properties props = loadConfig();
        int current = parseIntSafe(props.getProperty(KEY_POINTS, "0"));
        if (current < cost) {
            return false;
        }
        props.setProperty(KEY_POINTS, String.valueOf(current - cost));
        saveConfig(props);
        if (onStateUpdated != null) {
            onStateUpdated.run();
        }
        return true;
    }

    public synchronized boolean verifyParentPin(String enteredPin) {
        Properties props = loadConfig();
        String savedPin = props.getProperty(KEY_PIN, "1234");
        return savedPin.equals(enteredPin);
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        File file = new File(context.getFilesDir(), CONFIG_FILE);
        if (!file.exists()) {
            props.setProperty(KEY_POINTS, "0");
            props.setProperty(KEY_PIN, "1234");
            saveConfig(props);
            return props;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (Exception ignored) {
        }
        return props;
    }

    private void saveConfig(Properties props) {
        File file = new File(context.getFilesDir(), CONFIG_FILE);
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            props.store(fos, "Edu Kit Configuration");
        } catch (Exception ignored) {
        }
    }

    private int parseIntSafe(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
