import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends Activity {

    private ConfigManager configManager;
    private Button parentBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configManager = new ConfigManager(this);

        // Optional: wire a button in activity_main.xml with id "parent_btn"
        parentBtn = findViewById(R.id.parent_btn);
        if (parentBtn != null) {
            parentBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptParentPin();
                }
            });
        }

        checkFirstBootPin();
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPin = input.getText().toString().trim();
                if (newPin.matches("\\d{4}")) {
                    configManager.setPin(newPin);
                    Toast.makeText(MainActivity.this, "Parent PIN saved", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    input.setError("PIN must be 4 digits");
                }
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
        String[] options = {"Change PIN", "Add +60s Screen Time"};
        new AlertDialog.Builder(this)
                .setTitle("Parent Controls")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showSetPinDialog(false);
                    } else if (which == 1) {
                        int updated = configManager.getSeconds() + 60;
                        configManager.setSeconds(updated);
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
                writeConfig("", 0);
            }
        }

        public String getPin() {
            return readField("pin", "");
        }

        public void setPin(String pin) {
            writeConfig(pin, getSeconds());
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
            writeConfig(getPin(), Math.max(0, seconds));
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

        private void writeConfig(String pin, int seconds) {
            try (FileWriter writer = new FileWriter(configFile, false)) {
                writer.write("pin=" + (pin == null ? "" : pin.trim()) + "\n");
                writer.write("seconds=" + Math.max(0, seconds) + "\n");
            } catch (IOException ignored) {
            }
        }
    }
}
