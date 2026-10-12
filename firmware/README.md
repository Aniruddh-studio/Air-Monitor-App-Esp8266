# ESP8266 Firmware — Air Monitor

This directory contains the Arduino C++ firmware for the **NodeMCU ESP8266** microcontroller driving the air quality monitoring and ventilation control system.

---

## Hardware Components

| Component | Model | Purpose |
| :--- | :--- | :--- |
| **Microcontroller** | NodeMCU ESP8266 (AMICA CP2102) | Wi-Fi REST server, sensor acquisition, relay control |
| **Dust Sensor** | Sharp GP2Y1010AU0F | Measures ambient particulate matter (PM2.5 / dust density) |
| **Temp & Humidity** | DHT22 (AM2302) | Measures ambient temperature (°C) and relative humidity (%) |
| **Actuator Relay** | 5V 1-Channel Relay Module | Controls high-voltage fan / air purifier power |
| **Status LEDs** | 5mm LEDs (Green, Yellow, Red) | Visual air quality indicator with 150Ω current-limiting resistors |

---

## Wiring & Pinout Reference

### 1. NodeMCU ESP8266 Pin Connections

| Module Pin | NodeMCU Pin | Function / Description |
| :--- | :--- | :--- |
| **GP2Y1010AU0F VCC** | `Vin` (5V) | Sensor power supply |
| **GP2Y1010AU0F GND** | `GND` | Ground |
| **GP2Y1010AU0F LED** | `D3` (GPIO 0) | Pulse control for internal IRED |
| **GP2Y1010AU0F Vo**  | `A0` (ADC0)   | Analog voltage output |
| **DHT22 VCC**        | `3V3`         | Sensor power supply |
| **DHT22 GND**        | `GND`         | Ground |
| **DHT22 DATA**       | `D4` (GPIO 2) | One-wire digital data pin |
| **Fan Relay SIG**    | `D1` (GPIO 5) | Active-LOW relay trigger signal |
| **Fan Relay VCC**    | `Vin` (5V) / `3V3` | Relay coil power (if 5V supported) |
| **Fan Relay GND**    | `GND`         | Ground |
| **Status LED Green** | `D6` (GPIO 12)| Connect through 150Ω resistor to LED anode -> GND |
| **Status LED Yellow**| `D7` (GPIO 13)| Connect through 150Ω resistor to LED anode -> GND |
| **Status LED Red**   | `D5` (GPIO 14)| Connect through 150Ω resistor to LED anode -> GND |

---

### 2. Fan Relay Logic & High-Voltage Wiring

- **Relay Logic**:
  - **Fan ON**: `D1` is set to `INPUT` (high impedance), releasing the pin so the relay stays deactivated. `COM` and `NC` remain connected.
  - **Fan OFF**: `D1` is set to `OUTPUT` and pulled `LOW`, activating the relay coil and breaking the `COM-NC` circuit.
- **AC Fan Wiring**:
  - `230V Phase (Mains)` -> Relay `COM`
  - Relay `NC` -> Fan Phase
  - `Neutral (Mains)` -> Fan Neutral
  - Relay `NO` -> Unused

---

### 3. Dust Density Calculation & Status LEDs

The firmware samples the analog output of the **Sharp GP2Y1010AU0F** with a 280µs pulse:
```cpp
voltage = raw * (5.0 / 1024.0);
dustDensity = 170.0 * voltage - 0.1; // µg/m³
if (dustDensity < 0) dustDensity = 0;
```

**LED Air Quality Thresholds**:
- **Green LED** (`D6`): `0` to `12.0 µg/m³` (Good Air Quality)
- **Yellow LED** (`D7`): `12.1` to `35.4 µg/m³` (Moderate)
- **Red LED** (`D5`): `>= 35.5 µg/m³` (Unhealthy / Hazardous)

---

## Software & Flashing Instructions

### Prerequisites
1. **Arduino IDE**: Version 1.8.19+ or Arduino IDE 2.x.
2. **ESP8266 Board Package**:
   - Go to **File** -> **Preferences** -> **Additional Board Manager URLs**.
   - Add: `http://arduino.esp8266.com/stable/package_esp8266com_index.json`
   - Open **Tools** -> **Board** -> **Boards Manager**, search for `esp8266` and install.
3. **Required Libraries** (Install via **Sketch** -> **Include Library** -> **Manage Libraries**):
   - `ArduinoJson` by Benoit Blanchon (**Version 7.x**)
   - `DHT sensor library` by Adafruit
   - `Adafruit Unified Sensor`

---

### Setup & Configuration

1. Open `firmware/air-monitor/air-monitor.ino` in Arduino IDE.
2. Update Wi-Fi credentials in lines 93–94:
   ```cpp
   const char* WIFI_SSID = "YOUR_WIFI_NAME";
   const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
   ```
3. *(Optional)* Configure static IP settings in `setup()` if desired:
   ```cpp
   IPAddress local_IP(192, 168, 1, 20);
   IPAddress gateway(192, 168, 1, 1);
   IPAddress subnet(255, 255, 255, 0);
   IPAddress dns(192, 168, 1, 1);
   ```
4. Under **Tools**, select:
   - **Board**: `NodeMCU 1.0 (ESP-12E Module)`
   - **Upload Speed**: `115200`
   - **CPU Frequency**: `80 MHz`
   - **Flash Size**: `4MB (FS:2MB OTA:~1019KB)`
   - **Port**: Select the COM port corresponding to your CP2102.
5. Click **Upload** (`Ctrl + U`).

---

## REST Endpoints Exposed

| Endpoint | Method | Response / Action |
| :--- | :--- | :--- |
| `/` | `GET` | HTML browser diagnostics dashboard |
| `/status` | `GET` | JSON containing `{ fan, tempC, humidityPct, dustUgM3, wifi }` |
| `/toggle/fan` | `POST` | Body: `{"on": true}` or `{"on": false}`. Toggles purifier relay |

**mDNS Support**: Once connected, the ESP8266 responds to `http://airmonitor.local/`.
