# Bluetooth Mouse App

This repository contains a standalone Android application (`MouseApp.apk`) that turns your phone into a Bluetooth mouse without needing *any* server application running on your computer.

It utilizes the Android `BluetoothHidDevice` API (available on Android 9/Pie and above) to broadcast a Bluetooth Human Interface Device profile, allowing it to natively emulate a Bluetooth mouse that can connect directly to PCs, Macs, tablets, or even other phones.

## Features
- Complete standalone functionality - no Python server required!
- Acts exactly like a physical Bluetooth mouse to the receiving computer.
- Provides a large trackpad and left/right click buttons.

## Requirements
- An Android device running Android 9.0 (Pie) or higher.
- A receiving computer or device that supports connecting to standard Bluetooth mice.

## Installation and Usage
1. Transfer `MouseApp.apk` from this repository to your Android phone.
2. Install it. (You may need to allow "Install from unknown sources" in settings).
3. Open the "BT Mouse" app on your phone.
4. If prompted, grant the required Bluetooth permissions and ensure Bluetooth is turned on.
5. The app will say "Ready to pair! Go to your PC's Bluetooth settings and pair with this phone."
6. On your computer, open Bluetooth settings and pair with your phone as you would a normal device.
7. Once connected, use the trackpad and buttons to move your mouse!

## Note
This replaced an older version of the app that required a Python backend over Wi-Fi. It is now a 100% native Android solution.
