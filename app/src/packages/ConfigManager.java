package com.edukit.launcher;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class ConfigManager {
    private static final String FILE_NAME = "config.txt";

    public static void saveConfig(Context context, int screenTimePoint, String pin) {
        int safePoints = Math.max(0, screenTimePoint);
        String cleanPin = (pin == null || pin.trim().isEmpty()) ? "1234" : pin.trim();

        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            String data = "screentimepoint=" + safePoints + "\n" +
                          "pin=" + cleanPin + "\n";
            fos.write(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getScreenTime(Context context) {
        ensureConfigExists(context);
        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("screentimepoint=")) {
                    return Math.max(0, Integer.parseInt(line.split("=", 2)[1].trim()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getPin(Context context) {
        ensureConfigExists(context);
        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("pin=")) {
                    return line.split("=", 2)[1].trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "1234";
    }

    public static void updateScreenTime(Context context, int newPoints) {
        saveConfig(context, newPoints, getPin(context));
    }

    private static void ensureConfigExists(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            saveConfig(context, 0, "1234");
        }
    }
}
