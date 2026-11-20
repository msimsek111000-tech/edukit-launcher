package com.edukit.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvTimeBank;
    private WebView eduWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTimeBank = findViewById(R.id.tvTimeBank);
        Button btnOpenEduApp = findViewById(R.id.btnOpenEduApp);
        Button btnSpendTime = findViewById(R.id.btnSpendTime);
        Button btnParentMode = findViewById(R.id.btnParentMode);
        eduWebView = findViewById(R.id.eduWebView);

        WebSettings settings = eduWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        eduWebView.addJavascriptInterface(new EduBridge(this, this::refreshTimeDisplay), "EduBridge");

        eduWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String appsJson = getAvailableEduAppsJson();
                view.loadUrl("javascript:if(window.loadEduApps){window.loadEduApps(" + appsJson + ");}");
            }
        });

        refreshTimeDisplay();

        btnOpenEduApp.setOnClickListener(v -> {
            eduWebView.setVisibility(View.VISIBLE);
            eduWebView.loadUrl("file:///android_asset/edu_webapp/index.html");
        });

        btnSpendTime.setOnClickListener(v -> {
            int points = ConfigManager.getScreenTime(this);
            if (points > 0) {
                Intent serviceIntent = new Intent(this, ScreenTimeService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                openOriginalLauncher();
            } else {
                Toast.makeText(this, "0 seconds left! Earn time in Edu Kit first.", Toast.LENGTH_SHORT).show();
            }
        });

        btnParentMode.setOnClickListener(v -> showPinDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopService(new Intent(this, ScreenTimeService.class));
        refreshTimeDisplay();
    }

    private String getAvailableEduAppsJson() {
        StringBuilder json = new StringBuilder("[");
        try {
            AssetManager assets = getAssets();
            String[] folders = assets.list("edu_webapp/apps");
            if (folders != null) {
                boolean first = true;
                for (String folder : folders) {
                    String[] files = assets.list("edu_webapp/apps/" + folder);
                    if (files != null) {
                        for (String file : files) {
                            if (file.endsWith(".html")) {
                                if (!first) json.append(",");
                                String cleanName = folder.replace("_", " ");
                                json.append("{\"name\":\"")
                                    .append(cleanName)
                                    .append("\",\"path\":\"apps/")
                                    .append(folder)
                                    .append("/")
                                    .append(file)
                                    .append("\"}");
                                first = false;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    private void refreshTimeDisplay() {
        int points = ConfigManager.getScreenTime(this);
        tvTimeBank.setText("Screen Time: " + points + "s");
    }

    private void openOriginalLauncher() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> matches = pm.queryIntentActivities(homeIntent, 0);

        for (ResolveInfo info : matches) {
            String pkg = info.activityInfo.packageName;
            if (!pkg.equals(getPackageName())) {
                Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    return;
                }
            }
        }

        Toast.makeText(this, "No other launcher found on this device.", Toast.LENGTH_SHORT).show();
    }

    private void showPinDialog() {
        EditText pinInput = new EditText(this);
        pinInput.setHint("Enter PIN");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Parent Mode")
                .setMessage("Enter PIN to manage time or PIN")
                .setView(pinInput)
                .setPositiveButton("Unlock", (dialog, which) -> {
                    String entered = pinInput.getText().toString().trim();
                    if (entered.equals(ConfigManager.getPin(this))) {
                        showParentControls();
                    } else {
                        Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showParentControls() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText secondsInput = new EditText(this);
        secondsInput.setHint("Set screentimepoint (seconds)");
        secondsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        secondsInput.setText(String.valueOf(ConfigManager.getScreenTime(this)));

        EditText newPinInput = new EditText(this);
        newPinInput.setHint("Set new PIN");
        newPinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        newPinInput.setText(ConfigManager.getPin(this));

        layout.addView(secondsInput);
        layout.addView(newPinInput);

        new AlertDialog.Builder(this)
                .setTitle("Parent Controls")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    int newSec = 0;
                    try {
                        newSec = Integer.parseInt(secondsInput.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}
                    String newPin = newPinInput.getText().toString().trim();
                    ConfigManager.saveConfig(this, newSec, newPin);
                    refreshTimeDisplay();
                    Toast.makeText(this, "Saved to config.txt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (eduWebView != null && eduWebView.getVisibility() == View.VISIBLE) {
            if (eduWebView.canGoBack()) {
                eduWebView.goBack();
            } else {
                eduWebView.setVisibility(View.GONE);
            }
        }
    }
}
