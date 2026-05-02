from flask import Flask, render_template
from flask_socketio import SocketIO
from pynput.mouse import Controller as MouseController, Button
from pynput.keyboard import Controller as KeyboardController, Key
import time
import threading
import json
import socket
import sys

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins="*")

mouse = MouseController()
keyboard = KeyboardController()

def handle_event(event_name, data):
    if event_name == 'mouse_move':
        dx = data.get('dx', 0)
        dy = data.get('dy', 0)
        mouse.move(dx, dy)
    elif event_name == 'mouse_click':
        button = data.get('button', 'left')
        action = data.get('action', 'click')

        b = Button.left if button == 'left' else Button.right if button == 'right' else Button.middle

        if action == 'click':
            mouse.click(b)
        elif action == 'press':
            mouse.press(b)
        elif action == 'release':
            mouse.release(b)
    elif event_name == 'mouse_scroll':
        dx = data.get('dx', 0)
        dy = data.get('dy', 0)
        mouse.scroll(dx, dy)
    elif event_name == 'key_press':
        key = data.get('key')
        if not key:
            return

        special_keys = {
            'Enter': Key.enter,
            'Backspace': Key.backspace,
            'Space': Key.space,
            'Tab': Key.tab,
            'Escape': Key.esc,
            'Shift': Key.shift,
            'Control': Key.ctrl,
            'Alt': Key.alt,
            'Meta': Key.cmd,
            'ArrowUp': Key.up,
            'ArrowDown': Key.down,
            'ArrowLeft': Key.left,
            'ArrowRight': Key.right,
        }

        try:
            if key in special_keys:
                keyboard.press(special_keys[key])
                keyboard.release(special_keys[key])
            else:
                keyboard.press(key)
                keyboard.release(key)
        except Exception as e:
            print(f"Error pressing key {key}: {e}")

@app.route('/')
def index():
    return render_template('index.html')

@socketio.on('mouse_move')
def handle_mouse_move(data):
    handle_event('mouse_move', data)

@socketio.on('mouse_click')
def handle_mouse_click(data):
    handle_event('mouse_click', data)

@socketio.on('mouse_scroll')
def handle_mouse_scroll(data):
    handle_event('mouse_scroll', data)

@socketio.on('key_press')
def handle_key_press(data):
    handle_event('key_press', data)

def bluetooth_server():
    try:
        # Check if AF_BLUETOOTH is available (it is on Linux)
        if not hasattr(socket, 'AF_BLUETOOTH'):
            print("Bluetooth sockets not available on this platform.")
            return

        server_sock = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
        server_sock.bind((socket.BDADDR_ANY, 1))
        server_sock.listen(1)

        print("Bluetooth RFCOMM server started. Listening on port 1...")

        while True:
            client_sock, address = server_sock.accept()
            print(f"Accepted connection from {address}")

            buffer = ""
            try:
                while True:
                    data = client_sock.recv(1024)
                    if not data:
                        break

                    buffer += data.decode('utf-8')

                    # Split by newline (assuming one JSON object per line)
                    messages = buffer.split('\n')
                    buffer = messages.pop() # Keep the incomplete message (if any) in buffer

                    for msg in messages:
                        if msg.strip():
                            try:
                                payload = json.loads(msg)
                                event_name = payload.get('event')
                                event_data = payload.get('data', {})
                                if event_name:
                                    handle_event(event_name, event_data)
                            except json.JSONDecodeError:
                                print(f"Invalid JSON received over BT: {msg}")
            except Exception as e:
                print(f"Bluetooth connection error: {e}")
            finally:
                client_sock.close()
                print(f"Closed connection from {address}")
    except Exception as e:
        print(f"Failed to start Bluetooth server: {e}")

if __name__ == '__main__':
    # Start Bluetooth server in a separate thread
    bt_thread = threading.Thread(target=bluetooth_server, daemon=True)
    bt_thread.start()

    # Start Web server
    socketio.run(app, host='0.0.0.0', port=5000, allow_unsafe_werkzeug=True)
