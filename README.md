# Air Monitor ESP8266 & Android Companion App

A complete end-to-end IoT indoor air quality monitoring ecosystem connecting an **ESP8266** Wi-Fi microcontroller to an Android companion app built with **Jetpack Compose** and **Material 3**.

---

## 📁 Repository Structure

```
Air-Monitor-App-Esp8266/
├── android-app/                 # Android Studio project (Jetpack Compose & Material 3)
│   ├── app/                     # Application module source code & resources
│   ├── gradle/                  # Gradle wrapper & version catalogs
│   ├── mock.py                  # Standalone Python ESP8266 HTTP mock server
│   ├── build.gradle.kts         # Gradle build script
│   ├── local.properties.example # Template for Android SDK & local properties
│   └── README.md                # Detailed Android app documentation
├── firmware/                    # ESP8266 NodeMCU Firmware
│   ├── air-monitor/             # Arduino sketch directory
│   │   └── air-monitor.ino      # Main Arduino C++ firmware sketch
│   └── README.md                # Hardware wiring, pinout & flashing guide
├── LICENSE                      # MIT License
└── README.md                    # Repository overview & system architecture
```

---

## 📡 Hardware & Firmware (`firmware/`)

The firmware is located in [`firmware/air-monitor/`](./firmware/air-monitor/):

- **Target Board**: NodeMCU ESP8266 (AMICA CP2102).
- **Sensors**:
  - **Sharp GP2Y1010AU0F**: Optical dust sensor measuring airborne particulate density ($\mu g/m^3$).
  - **DHT22 (AM2302)**: Digital sensor measuring ambient temperature (°C) and relative humidity (%).
- **Actuator Control**: 5V Relay module (**JQC3F-05VDC-C**) driving high-voltage air purifier / ventilation fan.
- **Visual Status LEDs**: Direct LED triage (Green: Good, Yellow: Moderate, Red: Unhealthy).
- **Web & REST Server**: Serves live JSON data at `/status`, processes purifier toggles at `/toggle/fan`, and serves a browser status page on `/`.
- **mDNS Discovery**: Resolves as `http://airmonitor.local/` on the local network.

For complete wiring diagrams, pinout mappings, and Arduino IDE setup, see [**Firmware Documentation**](./firmware/README.md).

---

## 📱 Android App (`android-app/`)

The Android application is located in the [`android-app/`](./android-app/) folder:

- **UI Framework**: Modern declarative UI with **Jetpack Compose** & **Material 3**.
- **Dynamic AQI Gauge**: Live circular air quality index dial calibrated against the **US EPA PM2.5 standard**.
- **Live Environmental Telemetry**: Real-time readouts for PM2.5 ($\mu g/m^3$), Temperature (°C), and Humidity (%).
- **Hardware Fan / Purifier Toggle**: HTTP POST control to switch the purifier relay on/off.
- **Offline Mock Server (`mock.py`)**: Included interactive CLI simulator to test app features without physical hardware.

For detailed build, setup, and testing instructions, please refer to the [**Android App Documentation**](./android-app/README.md).

---

## 🔌 Hardware Architecture & API

```
[ ESP8266 Microcontroller ]
   ├── Sharp Optical Dust Sensor (GP2Y1010AU0F)
   ├── Temperature & Humidity Sensor (DHT22)
   ├── 3x Air Quality Status LEDs (Green, Yellow, Red)
   └── 5V Relay Module (JQC3F-05VDC-C, Controls Fan / Air Purifier)
            │
            ▼ Wi-Fi (HTTP REST over LAN)
            │
[ Android Device (AQI Sensor App) ]
```

### ESP8266 HTTP API Endpoints

- **`GET /status`**: Returns JSON with current sensor readings:
  ```json
  {
    "fan": true,
    "tempC": 24.5,
    "humidityPct": 58.0,
    "dustUgM3": 18.2,
    "wifi": true
  }
  ```
- **`POST /toggle/fan`**: Toggles purifier relay on/off:
  ```json
  {
    "on": true
  }
  ```

---

## 🚀 Quick Start

### 1. Flash the Firmware
1. Open [`firmware/air-monitor/air-monitor.ino`](./firmware/air-monitor/air-monitor.ino) in Arduino IDE.
2. Update `WIFI_SSID` and `WIFI_PASSWORD` with your local Wi-Fi credentials.
3. Install required libraries (`ArduinoJson` v7, `DHT sensor library`).
4. Select board **NodeMCU 1.0 (ESP-12E Module)** and upload.
5. Check Serial Monitor (`115200 baud`) to retrieve the assigned IP address.

### 2. Run the Android App
1. Open Android Studio and choose **Open**, then select the `android-app` directory.
2. Copy `local.properties.example` to `local.properties` inside `android-app/` and verify your Android SDK path.
3. Build and launch:
   ```bash
   cd android-app
   ./gradlew assembleDebug
   ```
4. Enter the ESP8266 IP address in the app settings to begin live streaming.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
