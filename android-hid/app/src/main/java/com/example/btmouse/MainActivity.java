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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BtMouse";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedHost;

    private TextView statusText;
    private View trackpad;
    private Button leftClickBtn;
    private Button rightClickBtn;
    private Button connectBtn;
    private Button disconnectBtn;

    // Standard Mouse HID Report Descriptor
    private static final byte[] MOUSE_REPORT_DESC = {
            0x05, 0x01, // Usage Page (Generic Desktop)
            0x09, 0x02, // Usage (Mouse)
            (byte) 0xA1, 0x01, // Collection (Application)
            0x09, 0x01, // Usage (Pointer)
            (byte) 0xA1, 0x00, // Collection (Physical)
            0x05, 0x09, // Usage Page (Button)
            0x19, 0x01, // Usage Minimum (1)
            0x29, 0x03, // Usage Maximum (3)
            0x15, 0x00, // Logical Minimum (0)
            0x25, 0x01, // Logical Maximum (1)
            (byte) 0x95, 0x03, // Report Count (3)
            0x75, 0x01, // Report Size (1)
            (byte) 0x81, 0x02, // Input (Data, Variable, Absolute)
            (byte) 0x95, 0x01, // Report Count (1)
            0x75, 0x05, // Report Size (5)
            (byte) 0x81, 0x03, // Input (Constant, Variable, Absolute) - padding
            0x05, 0x01, // Usage Page (Generic Desktop)
            0x09, 0x30, // Usage (X)
            0x09, 0x31, // Usage (Y)
            0x15, (byte) 0x81, // Logical Minimum (-127)
            0x25, 0x7F, // Logical Maximum (127)
            0x75, 0x08, // Report Size (8)
            (byte) 0x95, 0x02, // Report Count (2)
            (byte) 0x81, 0x06, // Input (Data, Variable, Relative)
            (byte) 0xC0, // End Collection
            (byte) 0xC0  // End Collection
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        trackpad = findViewById(R.id.trackpad);
        leftClickBtn = findViewById(R.id.btn_left_click);
        rightClickBtn = findViewById(R.id.btn_right_click);
        connectBtn = findViewById(R.id.btn_connect);
        disconnectBtn = findViewById(R.id.btn_disconnect);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkPermissionsAndInit();

        connectBtn.setOnClickListener(v -> showPairedDevicesDialog());
        disconnectBtn.setOnClickListener(v -> disconnect());
        setupTrackpad();
        setupButtons();
    }

    private void checkPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
                initHidDevice();
            } else {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initHidDevice() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 2);
            return;
        }

        statusText.setText("Initializing HID Profile...");

        bluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = (BluetoothHidDevice) proxy;
                    Log.d(TAG, "HID Device proxy connected");
                    registerApp();
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = null;
                    Log.d(TAG, "HID Device proxy disconnected");
                }
            }
        }, BluetoothProfile.HID_DEVICE);
    }

    @SuppressLint("MissingPermission")
    private void registerApp() {
        if (hidDevice == null) return;

        BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                "BT Mouse",
                "Virtual Mouse",
                "Google",
                BluetoothHidDevice.SUBCLASS1_MOUSE,
                MOUSE_REPORT_DESC
        );

        hidDevice.registerApp(
                sdpSettings,
                null,
                null,
                Executors.newSingleThreadExecutor(),
                hidCallback
        );
    }

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            Log.d(TAG, "App registered: " + registered);
            runOnUiThread(() -> {
                if (registered) {
                    statusText.setText("Ready! You can now pair or connect to a PC.");
                    connectBtn.setVisibility(View.VISIBLE);
                } else {
                    statusText.setText("App Registration Failed");
                }
            });
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            Log.d(TAG, "Connection state: " + state);
            runOnUiThread(() -> {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    connectedHost = device;
                    statusText.setText("Connected to: " + getDeviceNameSafe(device));
                    connectBtn.setVisibility(View.GONE);
                    disconnectBtn.setVisibility(View.VISIBLE);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    connectedHost = null;
                    statusText.setText("Disconnected. Ready to connect.");
                    connectBtn.setVisibility(View.VISIBLE);
                    disconnectBtn.setVisibility(View.GONE);
                }
            });
        }
    };

    @SuppressLint("MissingPermission")
    private void showPairedDevicesDialog() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
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
            statusText.setText("Connecting to " + getDeviceNameSafe(device) + "...");
            hidDevice.connect(device);
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
            hidDevice.disconnect(connectedHost);
        }
    }

    private float lastX, lastY;

    @SuppressLint("ClickableViewAccessibility")
    private void setupTrackpad() {
        trackpad.setOnTouchListener((v, event) -> {
            if (connectedHost == null || hidDevice == null) return true;

            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    sendMouseReport(0, (int) (dx * 1.5), (int) (dy * 1.5));
                    lastX = event.getX();
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    lastX = 0;
                    lastY = 0;
                    break;
            }
            return true;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButtons() {
        leftClickBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendMouseReport(1, 0, 0); // Left click press
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendMouseReport(0, 0, 0); // Release
            }
            return true;
        });

        rightClickBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendMouseReport(2, 0, 0); // Right click press
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendMouseReport(0, 0, 0); // Release
            }
            return true;
        });
    }

    @SuppressLint("MissingPermission")
    private void sendMouseReport(int buttons, int dx, int dy) {
        if (hidDevice == null || connectedHost == null) return;

        // Ensure deltas are within byte limits (-127 to 127)
        byte bDx = (byte) Math.max(-127, Math.min(127, dx));
        byte bDy = (byte) Math.max(-127, Math.min(127, dy));
        byte bBtns = (byte) buttons;

        byte[] report = new byte[]{bBtns, bDx, bDy};
        hidDevice.sendReport(connectedHost, 0, report);
    }
}
