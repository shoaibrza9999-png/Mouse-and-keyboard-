package com.example.btmouse;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BtMouse";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedHost;

    private boolean isRegistered = false;

    private TextView statusText;
    private TextView logText;
    private ScrollView logScrollView;
    private View trackpad;
    private Button leftClickBtn;
    private Button rightClickBtn;
    private Button connectBtn;
    private Button disconnectBtn;

    // Report IDs
    private static final int ID_KEYBOARD = 1;
    private static final int ID_MOUSE = 2;

    // Combo Keyboard + Mouse HID Report Descriptor
    private static final byte[] COMBO_REPORT_DESC = {
        // -------------------------------------------------
        // Keyboard Report (ID 1)
        // -------------------------------------------------
        0x05, 0x01,                         // Usage Page (Generic Desktop)
        0x09, 0x06,                         // Usage (Keyboard)
        (byte) 0xA1, 0x01,                  // Collection (Application)
        (byte) 0x85, ID_KEYBOARD,           //   Report ID (1)
        0x05, 0x07,                         //   Usage Page (Key Codes)
        0x19, (byte) 0xe0,                  //   Usage Minimum (224)
        0x29, (byte) 0xe7,                  //   Usage Maximum (231)
        0x15, 0x00,                         //   Logical Minimum (0)
        0x25, 0x01,                         //   Logical Maximum (1)
        0x75, 0x01,                         //   Report Size (1)
        (byte) 0x95, 0x08,                  //   Report Count (8)
        (byte) 0x81, 0x02,                  //   Input (Data, Variable, Absolute)
        (byte) 0x95, 0x01,                  //   Report Count (1)
        0x75, 0x08,                         //   Report Size (8)
        (byte) 0x81, 0x01,                  //   Input (Constant) reserved byte(1)
        (byte) 0x95, 0x05,                  //   Report Count (5)
        0x75, 0x01,                         //   Report Size (1)
        0x05, 0x08,                         //   Usage Page (Page# for LEDs)
        0x19, 0x01,                         //   Usage Minimum (1)
        0x29, 0x05,                         //   Usage Maximum (5)
        (byte) 0x91, 0x02,                  //   Output (Data, Variable, Absolute), Led report
        (byte) 0x95, 0x01,                  //   Report Count (1)
        0x75, 0x03,                         //   Report Size (3)
        (byte) 0x91, 0x01,                  //   Output (Data, Variable, Absolute), Led report padding
        (byte) 0x95, 0x06,                  //   Report Count (6)
        0x75, 0x08,                         //   Report Size (8)
        0x15, 0x00,                         //   Logical Minimum (0)
        0x25, 0x65,                         //   Logical Maximum (101)
        0x05, 0x07,                         //   Usage Page (Key codes)
        0x19, 0x00,                         //   Usage Minimum (0)
        0x29, 0x65,                         //   Usage Maximum (101)
        (byte) 0x81, 0x00,                  //   Input (Data, Array) Key array(6 bytes)
        (byte) 0xC0,                        // End Collection

        // -------------------------------------------------
        // Mouse Report (ID 2)
        // -------------------------------------------------
        0x05, 0x01,                         // Usage Page (Generic Desktop)
        0x09, 0x02,                         // Usage (Mouse)
        (byte) 0xA1, 0x01,                  // Collection (Application)
        (byte) 0x85, ID_MOUSE,              //   Report ID (2)
        0x09, 0x01,                         //   Usage (Pointer)
        (byte) 0xA1, 0x00,                  //   Collection (Physical)
        0x05, 0x09,                         //     Usage Page (Button)
        0x19, 0x01,                         //     Usage Minimum (1)
        0x29, 0x03,                         //     Usage Maximum (3)
        0x15, 0x00,                         //     Logical Minimum (0)
        0x25, 0x01,                         //     Logical Maximum (1)
        (byte) 0x95, 0x03,                  //     Report Count (3)
        0x75, 0x01,                         //     Report Size (1)
        (byte) 0x81, 0x02,                  //     Input (Data, Variable, Absolute)
        (byte) 0x95, 0x01,                  //     Report Count (1)
        0x75, 0x05,                         //     Report Size (5)
        (byte) 0x81, 0x03,                  //     Input (Constant, Variable, Absolute) - padding
        0x05, 0x01,                         //     Usage Page (Generic Desktop)
        0x09, 0x30,                         //     Usage (X)
        0x09, 0x31,                         //     Usage (Y)
        0x09, 0x38,                         //     Usage (Wheel)
        0x15, (byte) 0x81,                  //     Logical Minimum (-127)
        0x25, 0x7F,                         //     Logical Maximum (127)
        0x75, 0x08,                         //     Report Size (8)
        (byte) 0x95, 0x03,                  //     Report Count (3)
        (byte) 0x81, 0x06,                  //     Input (Data, Variable, Relative)
        (byte) 0xC0,                        //   End Collection
        (byte) 0xC0                         // End Collection
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        logText = findViewById(R.id.log_text);
        logScrollView = findViewById(R.id.log_scrollview);
        trackpad = findViewById(R.id.trackpad);
        leftClickBtn = findViewById(R.id.btn_left_click);
        rightClickBtn = findViewById(R.id.btn_right_click);
        connectBtn = findViewById(R.id.btn_connect);
        disconnectBtn = findViewById(R.id.btn_disconnect);

        addLog("App started");

        // Show warning about unpairing if this is the first run after updating
        SharedPreferences prefs = getSharedPreferences("BtMousePrefs", MODE_PRIVATE);
        boolean isUpdated = prefs.getBoolean("isComboUpdated", false);
        if (!isUpdated) {
            new AlertDialog.Builder(this)
                .setTitle("Important Update!")
                .setMessage("The Bluetooth Profile has been updated to fix clicking and scrolling bugs.\n\nYou MUST un-pair/forget this phone from your PC's Bluetooth settings, and pair it again! If you don't un-pair, your PC will remember the old buggy profile.")
                .setPositiveButton("I Understand", (dialog, which) -> {
                    prefs.edit().putBoolean("isComboUpdated", true).apply();
                })
                .setCancelable(false)
                .show();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            addLog("ERROR: Bluetooth not supported on this device.");
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkPermissionsAndInit();

        connectBtn.setOnClickListener(v -> handleConnectClick());
        disconnectBtn.setOnClickListener(v -> disconnect());
        setupTrackpad();
        setupButtons();
    }

    private void addLog(String msg) {
        Log.d(TAG, msg);
        runOnUiThread(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            logText.append("[" + time + "] " + msg + "\n");
            logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void checkPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                addLog("Requesting Bluetooth permissions...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE}, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }
        initHidDevice();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLog("Bluetooth permissions granted.");
                initHidDevice();
            } else {
                addLog("ERROR: Bluetooth permissions denied.");
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initHidDevice() {
        if (!bluetoothAdapter.isEnabled()) {
            addLog("Bluetooth is disabled, requesting to enable...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 2);
            return;
        }

        addLog("Getting HID Device profile proxy...");
        statusText.setText("Initializing HID Profile...");

        boolean success = bluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = (BluetoothHidDevice) proxy;
                    addLog("HID Device proxy connected.");
                    registerApp();
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = null;
                    isRegistered = false;
                    addLog("HID Device proxy disconnected.");
                }
            }
        }, BluetoothProfile.HID_DEVICE);

        if (!success) {
            addLog("ERROR: Failed to get HID profile proxy. Device may not support it.");
        }
    }

    @SuppressLint("MissingPermission")
    private void registerApp() {
        if (hidDevice == null) {
            addLog("ERROR: Cannot register app, HID proxy is null.");
            return;
        }

        addLog("Registering HID App...");
        BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                "BT Mouse",
                "Virtual Mouse",
                "Google",
                (byte) 0xC0, // SUBCLASS1_COMBO (Keyboard + Mouse)
                COMBO_REPORT_DESC
        );

        boolean success = hidDevice.registerApp(
                sdpSettings,
                null,
                null,
                Executors.newSingleThreadExecutor(),
                hidCallback
        );

        if (!success) {
            addLog("ERROR: registerApp() returned false.");
        }
    }

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            addLog("App registration status changed: " + registered);
            isRegistered = registered;
            runOnUiThread(() -> {
                if (registered) {
                    statusText.setText("Ready! You can now connect to a PC.");
                } else {
                    statusText.setText("App Registration Failed");
                }
            });
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            addLog("Connection state changed: " + state);
            runOnUiThread(() -> {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    connectedHost = device;
                    String name = getDeviceNameSafe(device);
                    addLog("Successfully connected to: " + name);
                    statusText.setText("Connected to: " + name);
                    connectBtn.setVisibility(View.GONE);
                    disconnectBtn.setVisibility(View.VISIBLE);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    connectedHost = null;
                    addLog("Disconnected.");
                    statusText.setText("Disconnected. Ready to connect.");
                    connectBtn.setVisibility(View.VISIBLE);
                    disconnectBtn.setVisibility(View.GONE);
                }
            });
        }
    };

    private void handleConnectClick() {
        if (hidDevice == null || !isRegistered) {
            addLog("Attempting to re-initialize HID profile...");
            initHidDevice();
            Toast.makeText(this, "Initializing Bluetooth Profile, please wait and try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        showPairedDevicesDialog();
    }

    @SuppressLint("MissingPermission")
    private void showPairedDevicesDialog() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            addLog("No paired devices found.");
            Toast.makeText(this, "No paired devices found. Pair with your PC first.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> deviceNames = new ArrayList<>();
        List<BluetoothDevice> deviceList = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(getDeviceNameSafe(device) + "\n" + device.getAddress());
            deviceList.add(device);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select PC to connect");
        builder.setItems(deviceNames.toArray(new CharSequence[0]), (dialog, which) -> {
            BluetoothDevice selectedDevice = deviceList.get(which);
            connectToDevice(selectedDevice);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (hidDevice != null) {
            String name = getDeviceNameSafe(device);
            addLog("Initiating connection to " + name + "...");
            statusText.setText("Connecting to " + name + "...");
            boolean success = hidDevice.connect(device);
            if (!success) {
                addLog("ERROR: hidDevice.connect() returned false.");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private String getDeviceNameSafe(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name != null ? name : "Unknown Device";
        } catch (Exception e) {
            return device.getAddress();
        }
    }

    @SuppressLint("MissingPermission")
    private void disconnect() {
        if (hidDevice != null && connectedHost != null) {
            addLog("Disconnecting from " + getDeviceNameSafe(connectedHost) + "...");
            hidDevice.disconnect(connectedHost);
        }
    }

    private int currentButtons = 0; // State of the buttons

    // Multi-touch tracking
    private int primaryPointerId = -1;
    private float lastX, lastY;
    private boolean isScrolling = false;
    private float scrollStartY;

    @SuppressLint("ClickableViewAccessibility")
    private void setupTrackpad() {
        trackpad.setOnTouchListener((v, event) -> {
            if (connectedHost == null || hidDevice == null) return true;

            int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // First finger down
                    primaryPointerId = event.getPointerId(0);
                    lastX = event.getX(0);
                    lastY = event.getY(0);
                    isScrolling = false;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    // Second finger down - initiate scroll mode
                    if (event.getPointerCount() == 2) {
                        isScrolling = true;
                        scrollStartY = event.getY(0); // Use first finger for scroll tracking
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    // Finger lifted
                    if (event.getPointerCount() == 2) {
                        isScrolling = false; // Transitioning back to 1 finger
                        // The remaining finger becomes the new primary pointer
                        int upIndex = event.getActionIndex();
                        int remainingIndex = upIndex == 0 ? 1 : 0;
                        primaryPointerId = event.getPointerId(remainingIndex);
                        lastX = event.getX(remainingIndex);
                        lastY = event.getY(remainingIndex);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isScrolling) {
                        // 2-finger scroll
                        float currentY = event.getY(0);
                        float dy = currentY - scrollStartY;

                        // Send scroll report if moved enough
                        if (Math.abs(dy) > 10) {
                            int scrollDir = (dy > 0) ? -1 : 1; // Natural scrolling
                            sendMouseReport(currentButtons, 0, 0, scrollDir);
                            scrollStartY = currentY; // Reset threshold
                        }
                    } else {
                        // 1-finger move
                        int pointerIndex = event.findPointerIndex(primaryPointerId);
                        if (pointerIndex != -1) {
                            float dx = event.getX(pointerIndex) - lastX;
                            float dy = event.getY(pointerIndex) - lastY;

                            // Send move report
                            sendMouseReport(currentButtons, (int) (dx * 1.5), (int) (dy * 1.5), 0);

                            lastX = event.getX(pointerIndex);
                            lastY = event.getY(pointerIndex);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    primaryPointerId = -1;
                    isScrolling = false;
                    break;
            }
            return true;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButtons() {
        leftClickBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                currentButtons |= 1; // Set bit 0
                sendMouseReport(currentButtons, 0, 0, 0);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                currentButtons &= ~1; // Clear bit 0
                sendMouseReport(currentButtons, 0, 0, 0);
            }
            return true;
        });

        rightClickBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                currentButtons |= 2; // Set bit 1
                sendMouseReport(currentButtons, 0, 0, 0);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                currentButtons &= ~2; // Clear bit 1
                sendMouseReport(currentButtons, 0, 0, 0);
            }
            return true;
        });
    }

    @SuppressLint("MissingPermission")
    private void sendMouseReport(int buttons, int dx, int dy, int dWheel) {
        if (hidDevice == null || connectedHost == null) return;

        // Ensure deltas are within signed byte limits (-127 to 127)
        byte bDx = (byte) Math.max(-127, Math.min(127, dx));
        byte bDy = (byte) Math.max(-127, Math.min(127, dy));
        byte bWheel = (byte) Math.max(-127, Math.min(127, dWheel));
        byte bBtns = (byte) buttons;

        // The payload for the mouse consists of 4 bytes: [Buttons, X, Y, Wheel]
        byte[] report = new byte[]{bBtns, bDx, bDy, bWheel};

        // Use ID_MOUSE (2)
        boolean success = hidDevice.sendReport(connectedHost, ID_MOUSE, report);
        if (!success) {
            // Uncomment to debug if reports are failing to send
            // Log.e(TAG, "Failed to send report");
        }
    }
}
