from flask import Flask, render_template
from flask_socketio import SocketIO
from pynput.mouse import Controller as MouseController, Button
from pynput.keyboard import Controller as KeyboardController, Key
import time

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins="*")

mouse = MouseController()
keyboard = KeyboardController()

@app.route('/')
def index():
    return render_template('index.html')

@socketio.on('mouse_move')
def handle_mouse_move(data):
    dx = data.get('dx', 0)
    dy = data.get('dy', 0)
    mouse.move(dx, dy)

@socketio.on('mouse_click')
def handle_mouse_click(data):
    button = data.get('button', 'left')
    action = data.get('action', 'click')

    b = Button.left if button == 'left' else Button.right if button == 'right' else Button.middle

    if action == 'click':
        mouse.click(b)
    elif action == 'press':
        mouse.press(b)
    elif action == 'release':
        mouse.release(b)

@socketio.on('mouse_scroll')
def handle_mouse_scroll(data):
    dx = data.get('dx', 0)
    dy = data.get('dy', 0)
    mouse.scroll(dx, dy)

@socketio.on('key_press')
def handle_key_press(data):
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

if __name__ == '__main__':
    socketio.run(app, host='0.0.0.0', port=5000, allow_unsafe_werkzeug=True)
