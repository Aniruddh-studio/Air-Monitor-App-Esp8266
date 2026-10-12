# AQI Sensor — Android Air Quality Monitor Companion

An elegant, modern Android companion application built with **Jetpack Compose** and **Material 3** for monitoring indoor air quality in real time. The app connects directly over local Wi-Fi to an **ESP8266** microcontroller equipped with dust, temperature, and humidity sensors, providing live metric gauges, historical trend charts, and hardware actuator control (air purifier / exhaust fan toggle).

---

## 📸 Key Features

- **Dynamic AQI Gauge**: Live circular air quality index dial calibrated against the **US EPA PM2.5 AQI standard** (0–500 scale with breakpoints for Good, Moderate, Unhealthy for Sensitive Groups, Unhealthy, Very Unhealthy, and Hazardous).
- **Comprehensive Environmental Telemetry**: Real-time readouts for:
  - PM2.5 Dust Density ($\mu g/m^3$)
  - Ambient Temperature (°C)
  - Relative Humidity (%)
- **Hardware Actuator Control**: Toggle your air purifier / fan relay directly from the mobile UI via HTTP POST.
- **Rolling Historical Trends**: On-device rolling buffer graphing real-time sensor shifts without putting historical storage strain on the microchip.
- **Battery-Aware Lifecycle**: Automatically pauses polling when backgrounded to preserve device battery.
- **Zero Hardcoded IPs or Secrets**: On first launch, the app prompts for the device's IP address and safely persists it using **Jetpack DataStore Preferences**. Can be updated at any time via Settings.
- **Offline Mock Server (`mock.py`)**: Includes an interactive Python CLI simulator to test all dashboard features and toggles without physical hardware.

---

## 🛠 Tech Stack & Architecture

- **UI Framework**: Modern declarative UI with **Jetpack Compose** and **Material 3**.
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow (UDF).
- **Networking**: **Retrofit 2**, **OkHttp 3** (with logging interceptor), and **Gson**.
- **Persistence**: **Jetpack DataStore Preferences** for local IP persistence.
- **Concurrency**: Kotlin **Coroutines** and reactive **StateFlow** / **SharedFlow**.
- **Typography & Assets**: JetBrains Mono & Inter via Google Downloadable Fonts; Lottie animations.
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with version catalogs (`libs.versions.toml`).

---

## 🔌 Hardware Architecture & Connection

The Android application operates on a **Local Area Network (LAN)** client-server model:

```
[ ESP8266 Microcontroller ]
   ├── Optical Dust Sensor (e.g., GP2Y1010AU0F / PMS5003)
   ├── DHT11 / DHT22 (Temp & Humidity Sensor)
   └── 5V Relay Module (Controls Air Purifier / Fan)
            ▲
            │ Wi-Fi (HTTP REST over LAN)
            ▼
[ Android Device (AQI Sensor App) ]
```

### ESP8266 REST API Specification

Your ESP8266 firmware must expose the following HTTP endpoints on port `80` (or configured port):

#### 1. Device Status
- **Method**: `GET`
- **Path**: `/status`
- **Response Format**: `application/json`
- **Response Payload**:
  ```json
  {
    "fan": true,
    "tempC": 24.5,
    "humidityPct": 58.0,
    "dustUgM3": 18.2,
    "wifi": true
  }
  ```

#### 2. Fan / Purifier Toggle
- **Method**: `POST`
- **Path**: `/toggle/fan`
- **Content-Type**: `application/json`
- **Request Payload**:
  ```json
  {
    "on": true
  }
  ```

---

## 🚀 Getting Started & Setup

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer recommended.
- **Android SDK**: Compile SDK `36`, Minimum SDK `24` (Android 7.0+).
- **JDK**: Java 11 or higher.

### 1. Clone the Repository
```bash
git clone https://github.com/Aniruddh-studio/Air-Monitor-App-Esp8266.git
cd Air-Monitor-App-Esp8266
```

### 2. Configure Local Properties & Secrets
Local configuration and machine-specific paths are excluded from version control via `.gitignore`.
1. Copy the example file to create your own `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
2. In `local.properties`, verify the path to your Android SDK:
   ```properties
   sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
   ```
3. *(Optional)* If you want a default device IP injected into debug builds:
   ```properties
   DEFAULT_DEVICE_IP=192.168.1.100
   ```

### 3. Build & Run
Open the project in Android Studio and click **Run 'app'** (`Shift + F10`) or build via Gradle:
```bash
./gradlew assembleDebug
```

---

## 🧪 Testing Without Hardware (`mock.py`)

A standalone mock ESP8266 server is included in the project root:

1. Open a terminal and run:
   ```bash
   python mock.py
   ```
2. The script starts an HTTP server on port `8080` and displays your machine's local IP address (e.g., `192.168.1.50:8080`).
3. Open the AQI Sensor app on your Android phone or emulator:
   - On physical device: ensure phone is on the same Wi-Fi and enter `<your-pc-ip>:8080`.
   - On Android emulator: enter `10.0.2.2:8080` (standard Android emulator alias for host localhost).
4. The terminal provides interactive commands to test real-time updates:
   - `temp 32.5` — Changes temperature
   - `hum 85` — Sets humidity percentage
   - `dust 180` — Simulates unhealthy dust spike to test AQI gauge reaction
   - `drift` — Toggles automatic wave drift animation
   - `stop` — Freezes readings

---

## 🛡 Security & Privacy

- **No Secrets in Source Control**: All machine paths, keystore keys, and local secrets remain in `local.properties` (strictly ignored by `.gitignore`).
- **Dynamic LAN Targeting**: No IP addresses or hardware MAC identifiers are hardcoded; connections are established dynamically through user-controlled settings stored in private app DataStore.
- **Cleartext LAN Traffic**: Android disables plain HTTP by default. This app enables LAN cleartext communication safely via `res/xml/network_security_config.xml` without exposing external cloud traffic.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) (or choose your preferred license).
