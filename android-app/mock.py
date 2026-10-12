"""
mock_esp8266.py  —  Fake AQI sensor server for testing without hardware
======================================================================
Mimics the ESP8266 HTTP API that the AQISensor Android app expects.

Endpoints served:
  GET  /status          → returns current fake sensor readings as JSON
  POST /toggle/fan      → toggles the fan on/off

The ESP8266 now sends raw dust density in µg/m³ — AQI calculation
is done entirely on the Android app side.

New /status JSON format:
  {
    "fan": true,
    "tempC": 23.8,
    "humidityPct": 73.0,
    "dustUgM3": 28.45,
    "wifi": true
  }

INTERACTIVE COMMANDS (type in the terminal while server is running)
-------------------------------------------------------------------
  temp  <value>     Set temperature    e.g.  temp 35.5
  hum   <value>     Set humidity %     e.g.  hum 80
  dust  <value>     Set dust µg/m³     e.g.  dust 200
  drift             Resume auto-drift (sine-wave) mode
  stop              Freeze values (disable auto-drift)
  status            Print current values
  help              Show this list

USAGE
-----
1. Run:  python mock_esp8266.py
2. Note the IP:port printed on screen
3. Enter that in the AQISensor app
4. Type commands below to change values live

Press Ctrl+C to stop.
"""

import json
import math
import random
import socket
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

# ─── Shared sensor state ────────────────────────────────────────────────────
state = {
    "fan":         True,
    "tempC":       24.0,
    "humidityPct": 55.0,
    "dustUgM3":    18.45,
    "wifi":        True,
}

# Drift mode: when True values auto-animate; when False values stay fixed
auto_drift   = True
_tick        = 0
_state_lock  = threading.Lock()

# ─── Sensor helpers ─────────────────────────────────────────────────────────

def _update_drift():
    """Drift values with sine waves when auto_drift is on."""
    global _tick
    if not auto_drift:
        return
    _tick += 1
    t = _tick * 0.05
    with _state_lock:
        state["tempC"]       = round(24.0 + 3.0 * math.sin(t * 0.7) + random.uniform(-0.1, 0.1), 1)
        state["humidityPct"] = round(55.0 + 10.0 * math.sin(t * 0.4) + random.uniform(-0.5, 0.5), 1)
        state["dustUgM3"]    = round(20.0 + 15.0 * math.sin(t * 0.3) + random.uniform(-1.0, 1.0), 2)
        state["humidityPct"] = max(10.0, min(99.0, state["humidityPct"]))
        state["dustUgM3"]    = max(0.0,  min(500.0, state["dustUgM3"]))

def _build_response() -> dict:
    _update_drift()
    with _state_lock:
        return {
            "fan":         state["fan"],
            "tempC":       state["tempC"],
            "humidityPct": state["humidityPct"],
            "dustUgM3":    state["dustUgM3"],
            "wifi":        state["wifi"],
        }

# ─── HTTP handler ────────────────────────────────────────────────────────────

class Handler(BaseHTTPRequestHandler):

    def _send_json(self, code: int, data: dict):
        body = json.dumps(data).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _read_body_json(self) -> dict:
        length = int(self.headers.get("Content-Length", 0))
        raw    = self.rfile.read(length) if length > 0 else b"{}"
        try:
            return json.loads(raw)
        except Exception:
            return {}

    def do_GET(self):
        if self.path == "/status":
            resp = _build_response()
            print(f"\r  [GET]  fan={'ON' if resp['fan'] else 'OFF'}  temp={resp['tempC']}°C  hum={resp['humidityPct']}%  "
                  f"dust={resp['dustUgM3']}µg/m³  wifi={'ON' if resp['wifi'] else 'OFF'}")
            print("  > ", end="", flush=True)
            self._send_json(200, resp)
        else:
            self._send_json(404, {"error": "not found"})

    def do_POST(self):
        body = self._read_body_json()
        on   = body.get("on", True)

        with _state_lock:
            if self.path == "/toggle/fan":
                state["fan"] = bool(on)
                print(f"\r  [POST] fan={'ON' if state['fan'] else 'OFF'}")
                self._send_json(200, {"fan": state["fan"]})

            else:
                self._send_json(404, {"error": "not found"})

        print("  > ", end="", flush=True)

    def log_message(self, fmt, *args):
        pass  # suppress default log

# ─── Interactive command loop (runs in main thread) ─────────────────────────

HELP = """
  Commands:
    temp  <value>   Set temperature  (e.g.  temp 35.5)
    hum   <value>   Set humidity %   (e.g.  hum 80)
    dust  <value>   Set dust µg/m³   (e.g.  dust 200.5)
    drift           Resume auto-drift mode
    stop            Freeze values (manual mode)
    status          Print current values
    help            Show this help
"""

def run_cli():
    global auto_drift
    print("  > ", end="", flush=True)
    while True:
        try:
            line = input().strip().lower()
        except EOFError:
            break

        parts = line.split()
        if not parts:
            print("  > ", end="", flush=True)
            continue

        cmd  = parts[0]
        arg  = parts[1] if len(parts) > 1 else None

        if cmd == "help":
            print(HELP)

        elif cmd == "drift":
            auto_drift = True
            print("  ✓ Auto-drift ON — values will animate automatically")

        elif cmd == "stop":
            auto_drift = False
            print("  ✓ Auto-drift OFF — values are now fixed")

        elif cmd == "status":
            with _state_lock:
                print(f"  temp={state['tempC']}°C  hum={state['humidityPct']}%  "
                      f"dust={state['dustUgM3']}µg/m³  fan={'ON' if state['fan'] else 'OFF'}  "
                      f"wifi={'ON' if state['wifi'] else 'OFF'}  "
                      f"drift={'ON' if auto_drift else 'OFF'}")

        elif cmd == "temp":
            if arg is None:
                print("  Usage: temp <value>  e.g.  temp 35.5")
            else:
                try:
                    val = round(float(arg), 1)
                    auto_drift = False
                    with _state_lock:
                        state["tempC"] = val
                    print(f"  ✓ Temperature set to {val}°C  (drift OFF)")
                except ValueError:
                    print(f"  ✗ '{arg}' is not a number")

        elif cmd == "hum":
            if arg is None:
                print("  Usage: hum <value>  e.g.  hum 80")
            else:
                try:
                    val = round(max(0.0, min(100.0, float(arg))), 1)
                    auto_drift = False
                    with _state_lock:
                        state["humidityPct"] = val
                    print(f"  ✓ Humidity set to {val}%  (drift OFF)")
                except ValueError:
                    print(f"  ✗ '{arg}' is not a number")

        elif cmd == "dust":
            if arg is None:
                print("  Usage: dust <value>  e.g.  dust 200.5")
            else:
                try:
                    val = round(max(0.0, min(500.0, float(arg))), 2)
                    auto_drift = False
                    with _state_lock:
                        state["dustUgM3"] = val
                    print(f"  ✓ Dust set to {val} µg/m³  (drift OFF)")
                except ValueError:
                    print(f"  ✗ '{arg}' is not a number")

        else:
            print(f"  Unknown command '{cmd}'. Type 'help' for a list.")

        print("  > ", end="", flush=True)

# ─── Entry point ─────────────────────────────────────────────────────────────

if __name__ == "__main__":
    PORT = 8080

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
    except Exception:
        local_ip = "127.0.0.1"

    print()
    print("=" * 52)
    print("  Mock ESP8266 AQI Sensor  —  port 8080")
    print("=" * 52)
    print(f"  Enter in app  :  {local_ip}:8080")
    print()
    print("  Type 'help' to see manual control commands")
    print("=" * 52)

    # Run HTTP server in a background daemon thread
    server = HTTPServer(("0.0.0.0", PORT), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    try:
        run_cli()          # blocking — runs in main thread
    except KeyboardInterrupt:
        print("\n  Server stopped.")
    finally:
        server.shutdown()
