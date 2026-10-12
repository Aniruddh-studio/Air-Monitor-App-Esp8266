# Air Monitor ESP8266 & Android Companion App

A complete indoor air quality monitoring ecosystem connecting an **ESP8266** Wi-Fi microcontroller to an Android companion app built with **Jetpack Compose** and **Material 3**.

---

## 📁 Repository Structure

```
Air-Monitor-App-Esp8266/
├── android-app/             # Android Studio project (Jetpack Compose & Material 3)
│   ├── app/                 # Application module source code & resources
│   ├── gradle/              # Gradle wrapper & version catalogs
│   ├── mock.py              # Standalone Python ESP8266 HTTP mock server
│   ├── build.gradle.kts     # Gradle build script
│   ├── local.properties.example # Template for Android SDK & local properties
│   └── README.md            # Detailed Android app documentation
├── LICENSE                  # MIT License
└── README.md                # Repository overview & hardware guide
```

---

## 📱 Android App (`android-app/`)

The Android application is located in the [`android-app`](./android-app) folder:

- **UI Framework**: Modern declarative UI with **Jetpack Compose** & **Material 3**.
- **Dynamic AQI Gauge**: Live air quality index dial calibrated against the **US EPA PM2.5 standard**.
- **Live Environmental Telemetry**: Real-time readouts for PM2.5 ($\mu g/m^3$), Temperature (°C), and Humidity (%).
- **Hardware Fan / Purifier Toggle**: HTTP POST control to switch the purifier relay on/off.
- **Offline Mock Server (`mock.py`)**: Included interactive CLI simulator to test app features without physical hardware.

For detailed build, setup, and testing instructions, please refer to the [**Android App Documentation**](./android-app/README.md).

---

## 🔌 Hardware Architecture & API

```
[ ESP8266 Microcontroller ]
   ├── Optical Dust Sensor (GP2Y1010AU0F / PMS5003)
   ├── Temperature & Humidity Sensor (DHT11 / DHT22)
   └── 5V Relay Module (Controls Fan / Air Purifier)
            ▲
            │ Wi-Fi (HTTP REST over LAN)
            ▼
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

## 🚀 Quick Start (Android App)

1. Open Android Studio and choose **Open**, then select the `android-app` directory.
2. Copy `local.properties.example` to `local.properties` inside `android-app/` and verify your Android SDK path.
3. Build and launch:
   ```bash
   cd android-app
   ./gradlew assembleDebug
   ```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
