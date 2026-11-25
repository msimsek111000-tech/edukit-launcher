import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static class AppEntry {
        final String label;
        final String packageName;
        final Drawable icon;

        AppEntry(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private ConfigManager configManager;
    private Button parentBtn;
    private TextView timeStatusTv;
    private ListView allowedAppsListView;
    private final List<AppEntry> displayedApps = new ArrayList<>();
    private ArrayAdapter<AppEntry> appsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configManager = new ConfigManager(this);
        timeStatusTv = findViewById(R.id.time_status_tv);
        parentBtn = findViewById(R.id.parent_btn);
        allowedAppsListView = findViewById(R.id.allowed_apps_list);

        appsAdapter = new ArrayAdapter<AppEntry>(this, android.R.layout.simple_list_item_1, displayedApps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = getLayoutInflater().inflate(android.R.layout.activity_list_item, parent, false);
                }
                AppEntry entry = getItem(position);
                ImageView iconView = row.findViewById(android.R.id.icon);
                TextView textView = row.findViewById(android.R.id.text1);
                if (entry != null) {
                    if (iconView != null && entry.icon != null) {
                        iconView.setImageDrawable(entry.icon);
                        iconView.setVisibility(View.VISIBLE);
                    }
                    if (textView != null) {
                        textView.setText(entry.label);
                        textView.setTextColor(Color.WHITE);
                    }
                }
                return row;
            }
        };

        if (allowedAppsListView != null) {
            allowedAppsListView.setAdapter(appsAdapter);
            allowedAppsListView.setOnItemClickListener((parent, view, position, id) -> {
                AppEntry clicked = displayedApps.get(position);
                launchAllowedApp(clicked);
            });
        }

        if (parentBtn != null) {
            parentBtn.setOnClickListener(v -> promptParentPin());
        }

        refreshUi();
        checkFirstBootPin();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        int secs = configManager.getSeconds();
        if (timeStatusTv != null) {
            timeStatusTv.setText("Screen Time: " + secs + "s");
        }
        loadAllowedApps();
    }

    private List<AppEntry> queryLaunchableApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolves = pm.queryIntentActivities(mainIntent, 0); //
        List<AppEntry> result = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();

        for (ResolveInfo info : resolves) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName()) || seenPackages.contains(pkg)) continue;
            seenPackages.add(pkg);
            CharSequence seq = info.loadLabel(pm);
            String label = seq != null ? seq.toString() : pkg;
            Drawable icon = info.loadIcon(pm);
            result.add(new AppEntry(label, pkg, icon));
        }

        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return result;
    }

    private void loadAllowedApps() {
        Set<String> allowedPackages = configManager.getAllowedPackages();
        List<AppEntry> allApps = queryLaunchableApps();

        displayedApps.clear();
        for (AppEntry app : allApps) {
            if (allowedPackages.contains(app.packageName)) {
                displayedApps.add(app);
            }
        }
        if (appsAdapter != null) {
            appsAdapter.notifyDataSetChanged();
        }
    }

    private void launchAllowedApp(AppEntry app) {
        if (configManager.getSeconds() <= 0) {
            Toast.makeText(this, "No screen time left! Complete tasks first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "Cannot launch " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAllowedAppsPicker() {
        List<AppEntry> allApps = queryLaunchableApps();
        if (allApps.isEmpty()) {
            Toast.makeText(this, "No launchable apps found", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> currentAllowed = configManager.getAllowedPackages();
        final String[] labels = new String[allApps.size()];
        final boolean[] checked = new boolean[allApps.size()];

        for (int i = 0; i < allApps.size(); i++) {
            labels[i] = allApps.get(i).label;
            checked[i] = currentAllowed.contains(allApps.get(i).packageName);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Allowed Apps")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked) //
                .setPositiveButton("Save Apps", (dialog, which) -> {
                    Set<String> updated = new HashSet<>();
                    for (int i = 0; i < allApps.size(); i++) {
                        if (checked[i]) {
                            updated.add(allApps.get(i).packageName);
                        }
                    }
                    configManager.setAllowedPackages(updated);
                    loadAllowedApps();
                    Toast.makeText(MainActivity.this, "Allowed apps updated (" + updated.size() + ")", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkFirstBootPin() {
        String savedPin = configManager.getPin();
        if (savedPin == null || savedPin.trim().isEmpty()) {
            showSetPinDialog(true);
        }
    }

    private void showSetPinDialog(final boolean isFirstBoot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isFirstBoot ? "First Boot: Set Parent PIN" : "Change Parent PIN");
        builder.setMessage("Enter a 4-digit PIN to protect launcher settings:");
        builder.setCancelable(!isFirstBoot);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        input.setHint("4-digit PIN");
        builder.setView(input);

        builder.setPositiveButton("Save PIN", null);
        if (!isFirstBoot) {
            builder.setNegativeButton("Cancel", null);
        }

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPin = input.getText().toString().trim();
            if (newPin.matches("\\d{4}")) {
                configManager.setPin(newPin);
                Toast.makeText(MainActivity.this, "Parent PIN saved", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                input.setError("PIN must be 4 digits");
            }
        });
    }

    private void promptParentPin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Parent Access");
        builder.setMessage("Enter Parent PIN:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        builder.setView(input);

        builder.setPositiveButton("Unlock", (dialog, which) -> {
            String entered = input.getText().toString().trim();
            String savedPin = configManager.getPin();
            if (savedPin != null && entered.equals(savedPin)) {
                openParentMenu();
            } else {
                Toast.makeText(MainActivity.this, "Wrong PIN", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openParentMenu() {
        String[] options = {"Manage Allowed Apps", "Change PIN", "Add +60s Screen Time"};
        new AlertDialog.Builder(this)
                .setTitle("Parent Controls")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAllowedAppsPicker();
                    } else if (which == 1) {
                        showSetPinDialog(false);
                    } else if (which == 2) {
                        int updated = configManager.getSeconds() + 60;
                        configManager.setSeconds(updated);
                        refreshUi();
                        Toast.makeText(MainActivity.this, "Screen time: " + updated + "s", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public static class ConfigManager {
        private static final String FILE_NAME = "config.txt";
        private final File configFile;

        public ConfigManager(Context context) {
            this.configFile = new File(context.getFilesDir(), FILE_NAME);
            if (!configFile.exists()) {
                writeConfig("", 0, new HashSet<String>());
            }
        }

        public String getPin() {
            return readField("pin", "");
        }

        public void setPin(String pin) {
            writeConfig(pin, getSeconds(), getAllowedPackages());
        }

        public int getSeconds() {
            String val = readField("seconds", "0");
            try {
                return Math.max(0, Integer.parseInt(val.trim()));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void setSeconds(int seconds) {
            writeConfig(getPin(), Math.max(0, seconds), getAllowedPackages());
        }

        public Set<String> getAllowedPackages() {
            String raw = readField("allowed_apps", "");
            Set<String> set = new HashSet<>();
            if (!raw.trim().isEmpty()) {
                for (String pkg : raw.split(",")) {
                    String trimmed = pkg.trim();
                    if (!trimmed.isEmpty()) {
                        set.add(trimmed);
                    }
                }
            }
            return set;
        }

        public void setAllowedPackages(Set<String> packages) {
            writeConfig(getPin(), getSeconds(), packages);
        }

        private String readField(String key, String fallback) {
            if (!configFile.exists()) return fallback;
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2 && parts[0].trim().equals(key)) {
                        return parts[1].trim();
                    }
                }
            } catch (IOException ignored) {
            }
            return fallback;
        }

        private void writeConfig(String pin, int seconds, Set<String> allowedPackages) {
            StringBuilder csv = new StringBuilder();
            if (allowedPackages != null) {
                for (String pkg : allowedPackages) {
                    if (pkg == null || pkg.trim().isEmpty()) continue;
                    if (csv.length() > 0) csv.append(",");
                    csv.append(pkg.trim());
                }
            }
            try (FileWriter writer = new FileWriter(configFile, false)) {
                writer.write("pin=" + (pin == null ? "" : pin.trim()) + "\n");
                writer.write("seconds=" + Math.max(0, seconds) + "\n");
                writer.write("allowed_apps=" + csv.toString() + "\n");
            } catch (IOException ignored) {
            }
        }
    }
}
