# Wireless Mouse & Keyboard App

This application turns your mobile phone into a wireless trackpad and keyboard for your computer. It consists of a **Python Backend Server** running on your computer and an **Android Mobile App** running on your phone.

## How It Works: Architecture Overview

The system is split into two main components that communicate with each other over either WiFi or Bluetooth:

### 1. The Python Backend Server (`app.py`)
This script runs on the computer you want to control.
- **Input Simulation:** It uses the `pynput` library to programmatically simulate physical mouse movements, clicks, scrolls, and keystrokes on the host operating system.
- **Listeners:** The server simultaneously listens for incoming connections via two methods:
  - **WiFi (WebSockets):** It runs a `Flask-SocketIO` web server (default port 5000) to receive low-latency JSON messages over a local network.
  - **Bluetooth (RFCOMM):** It runs a background thread that establishes an `AF_BLUETOOTH` server socket (on port 1) to accept direct Bluetooth serial connections and parse incoming JSON data.

### 2. The Android Mobile App (`MouseApp.apk`)
The mobile application is built using **Apache Cordova**, which wraps an HTML/JS/CSS web application into a native Android APK.
- **User Interface:** The interface provides a large trackpad area, a scroll wheel zone, left/middle/right click buttons, and a hidden input field to capture the mobile keyboard's keystrokes.
- **Touch Events:** The JavaScript captures touch events (`touchstart`, `touchmove`, `touchend`) and calculates the delta (difference) in coordinates to determine the speed and direction of your finger swipe.
- **Connection Logic:** Using the settings gear icon ⚙️, you can choose how to connect to the computer:
  - **WiFi:** Uses `socket.io-client` to connect to the computer's IP address.
  - **Bluetooth:** Uses the `cordova-plugin-bluetooth-serial` native plugin to list paired devices and establish a direct connection without relying on a local router.
- **Data Transmission:** When a movement or click is detected, the app packages the data into a JSON payload (e.g., `{ "event": "mouse_move", "data": { "dx": 5, "dy": -2 } }`) and transmits it over the active connection.

## Setup & Usage Instructions

### Running the Computer Server
1. Ensure you have Python installed.
2. Install the required dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Run the server script:
   ```bash
   python app.py
   ```
   *Note: Ensure your firewall allows traffic on port 5000 if using WiFi, and that your PC's Bluetooth is discoverable and paired if using Bluetooth.*

### Installing the Mobile App
1. Transfer the `MouseApp.apk` file located in the root of this repository to your Android phone.
2. Open the file on your phone and choose to install it. (You may need to allow "Install unknown apps" in your Android settings).
3. Open the "MouseApp" on your phone.

### Connecting
1. Tap the settings gear icon ⚙️ in the top right.
2. **For WiFi:** Select the "WiFi / IP" tab, enter your computer's local IP address (e.g., `192.168.1.5`), and click "Connect WiFi".
3. **For Bluetooth:**
   - Ensure your phone is paired with your PC via your phone's standard Bluetooth settings.
   - Select the "Bluetooth" tab inside the app.
   - Click "Refresh Devices", select your PC from the dropdown list, and click "Connect Bluetooth".
4. Once connected, swipe in the "TRACKPAD" area to move your mouse!
