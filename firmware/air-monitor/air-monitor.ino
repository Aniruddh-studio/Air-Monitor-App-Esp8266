
/*
  Humidity + Dust Monitor + Fan Control
  Board: NodeMCU ESP8266 (AMICA CP2102)

  ==========================================================
  STATUS LEDs
  ==========================================================

  Green  -> D6 -> 150 ohm -> LED -> GND
  Yellow -> D7 -> 150 ohm -> LED -> GND
  Red    -> D5 -> 150 ohm -> LED -> GND

  LED LOGIC:
  Dust Density 0 - 12.0 ug/m3       -> GREEN
  Dust Density 12.1 - 35.4 ug/m3    -> YELLOW
  Dust Density 35.5 ug/m3 and above -> RED

  No AQI calculation.

  ==========================================================
  GP2Y1010AU0F
  ==========================================================

  VCC      -> Vin (5V)
  GND      -> GND
  LED CTRL -> D3
  Vo       -> A0

  ==========================================================
  DHT22
  ==========================================================

  VCC  -> 3V3
  GND  -> GND
  DATA -> D4

  ==========================================================
  FAN RELAY
  ==========================================================

  SIG -> D1
  VCC -> 5V (only if supported by the module)
  GND -> GND

  Relay behavior established during testing:
  D1 INPUT          -> Relay deactivated -> COM-NC connected -> Fan ON
  D1 OUTPUT + LOW   -> Relay activated   -> COM-NC disconnected -> Fan OFF

  Fan wiring:
  230V Phase -> COM
  NC         -> Fan Phase
  Neutral    -> Fan Neutral
  NO         -> Unused

  ==========================================================
  GENERAL
  ==========================================================

  Dust and humidity monitoring are always active.
  Only the fan can be controlled by the app.

  /status returns:
  fan, tempC, humidityPct, dustUgM3, wifi

  No AQI or dust voltage is transmitted.
  Automatic Wi-Fi reconnection is included.
*/


// ==========================================================
// LIBRARIES
// ==========================================================

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <ArduinoJson.h>
#include <DHT.h>


// ==========================================================
// mDNS
// ==========================================================

const char* MDNS_HOSTNAME = "airmonitor";


// ==========================================================
// WI-FI CREDENTIALS
// ==========================================================

const char* WIFI_SSID = "YOUR_WIFI_NAME";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";


// ==========================================================
// PIN DEFINITIONS
// ==========================================================

#define DHTPIN D4
#define DHTTYPE DHT22

#define DUST_LED_PIN D3
#define DUST_ANALOG_PIN A0

#define STATUS_LED_GREEN D6
#define STATUS_LED_YELLOW D7
#define STATUS_LED_RED D5

#define FAN_RELAY_PIN D1


// ==========================================================
// OBJECTS
// ==========================================================

DHT dht(DHTPIN, DHTTYPE);
ESP8266WebServer server(80);


// ==========================================================
// GP2Y1010 TIMING
// ==========================================================

const int SAMPLING_TIME = 280;
const int DELTA_TIME = 40;
const int SLEEP_TIME = 9680;


// ==========================================================
// FAN STATE
// ==========================================================

// true = Fan ON
// false = Fan OFF

bool fanOn = true;


// ==========================================================
// WI-FI RECONNECTION
// ==========================================================

unsigned long lastWiFiCheck = 0;
const unsigned long WIFI_CHECK_INTERVAL = 10000;


// ==========================================================
// LAST READINGS
// ==========================================================

float lastTempC = NAN;
float lastHumid = NAN;
float lastDust = 0;


// ==========================================================
// FAN CONTROL
// ==========================================================

void setFan(bool state) {

  fanOn = state;

  if (fanOn) {

    // FAN ON:
    // Release D1 so it does not actively drive
    // the relay input.
    // Expected: relay deactivated, COM-NC connected.

    pinMode(FAN_RELAY_PIN, INPUT);

    Serial.println("Fan ON - relay pin released");

  } else {

    // FAN OFF:
    // Configure D1 as output and drive LOW.
    // Expected: active-LOW relay activates,
    // disconnecting COM-NC.

    pinMode(FAN_RELAY_PIN, OUTPUT);
    digitalWrite(FAN_RELAY_PIN, LOW);

    Serial.println("Fan OFF - relay activated");
  }
}


// ==========================================================
// AUTOMATIC WI-FI RECONNECTION
// ==========================================================

void checkWiFi() {

  if (millis() - lastWiFiCheck < WIFI_CHECK_INTERVAL) {
    return;
  }

  lastWiFiCheck = millis();

  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  Serial.println("Wi-Fi disconnected. Reconnecting...");

  WiFi.disconnect();
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}


// ==========================================================
// STATUS LED CONTROL
// ==========================================================

void updateStatusLED(float dustDensity) {

  // Turn all status LEDs OFF first.
  digitalWrite(STATUS_LED_GREEN, LOW);
  digitalWrite(STATUS_LED_YELLOW, LOW);
  digitalWrite(STATUS_LED_RED, LOW);

  if (dustDensity <= 12.0) {

    // Good
    digitalWrite(STATUS_LED_GREEN, HIGH);

  } else if (dustDensity <= 35.4) {

    // Moderate
    digitalWrite(STATUS_LED_YELLOW, HIGH);

  } else {

    // 35.5 ug/m3 and above
    digitalWrite(STATUS_LED_RED, HIGH);
  }
}


// ==========================================================
// JSON STATUS ENDPOINT
// ==========================================================

void handleStatus() {

  JsonDocument doc;

  doc["fan"] = fanOn;
  doc["tempC"] = isnan(lastTempC) ? 0 : lastTempC;
  doc["humidityPct"] = isnan(lastHumid) ? 0 : lastHumid;
  doc["dustUgM3"] = lastDust;
  doc["wifi"] = (WiFi.status() == WL_CONNECTED);

  String out;
  serializeJson(doc, out);

  server.send(200, "application/json", out);
}


// ==========================================================
// FAN CONTROL ENDPOINT
// ==========================================================

void handleToggleFan() {

  if (!server.hasArg("plain")) {

    server.send(
      400,
      "application/json",
      "{\"error\":\"bad body\"}"
    );

    return;
  }

  JsonDocument doc;

  DeserializationError err =
    deserializeJson(doc, server.arg("plain"));

  if (err) {

    server.send(
      400,
      "application/json",
      "{\"error\":\"invalid JSON\"}"
    );

    return;
  }

  if (!doc["on"].is<bool>()) {

    server.send(
      400,
      "application/json",
      "{\"error\":\"missing 'on' boolean\"}"
    );

    return;
  }

  bool on = doc["on"].as<bool>();

  setFan(on);

  handleStatus();
}


// ==========================================================
// BROWSER PAGE
// ==========================================================

void handleRoot() {

  String page =
    "<html><body style='font-family:sans-serif;"
    "text-align:center;padding-top:30px'>";

  page += "<h2>Air Monitor</h2>";

  page +=
    "<p>Fan: <b>" +
    String(fanOn ? "ON" : "OFF") +
    "</b></p>";

  page +=
    "<p>Temp: " +
    String(lastTempC, 1) +
    " C</p>";

  page +=
    "<p>Humidity: " +
    String(lastHumid, 1) +
    " %</p>";

  page +=
    "<p>Dust Density: " +
    String(lastDust, 2) +
    " ug/m3</p>";

  page +=
    "<p>Wi-Fi: <b>" +
    String(
      WiFi.status() == WL_CONNECTED
        ? "Connected"
        : "Disconnected"
    ) +
    "</b></p>";

  page +=
    "<p style='color:#888;font-size:13px'>"
    "Fan control endpoint: /toggle/fan"
    "</p>";

  page +=
    "<p style='color:#888;font-size:13px'>"
    "Live status: <a href='/status'>/status</a>"
    "</p>";

  page += "</body></html>";

  server.send(200, "text/html", page);
}


// ==========================================================
// READ GP2Y1010 AND CALCULATE DUST DENSITY
// ==========================================================
//
// Requested formula:
// voltage = raw * (5.0 / 1024.0)
// dustDensity = 170.0 * voltage - 0.1
//
// No AQI calculation.
// No averaging.
// Voltage is not transmitted.
//

float readDustDensity() {

  // Turn sensor LED ON (active LOW).
  digitalWrite(DUST_LED_PIN, LOW);

  delayMicroseconds(SAMPLING_TIME);

  int raw = analogRead(DUST_ANALOG_PIN);

  delayMicroseconds(DELTA_TIME);

  // Turn sensor LED OFF.
  digitalWrite(DUST_LED_PIN, HIGH);

  delayMicroseconds(SLEEP_TIME);

  float voltage = raw * (5.0 / 1024.0);

  float dustDensity = 170.0 * voltage - 0.1;

  if (dustDensity < 0) {
    dustDensity = 0;
  }

  return dustDensity;
}


// ==========================================================
// READING INTERVAL
// ==========================================================

unsigned long lastReadingTime = 0;
const unsigned long READING_INTERVAL = 2000;


// ==========================================================
// SETUP
// ==========================================================

void setup() {

  Serial.begin(115200);


  // ========================================================
  // FAN RELAY
  // ========================================================

  // Initialize in the tested fan-ON state:
  // D1 is released instead of being driven HIGH.

  pinMode(FAN_RELAY_PIN, INPUT);
  fanOn = true;

  Serial.println("Fan initialized ON - relay pin released");


  // ========================================================
  // GP2Y1010
  // ========================================================

  pinMode(DUST_LED_PIN, OUTPUT);
  digitalWrite(DUST_LED_PIN, HIGH);


  // ========================================================
  // STATUS LEDs
  // ========================================================

  pinMode(STATUS_LED_GREEN, OUTPUT);
  pinMode(STATUS_LED_YELLOW, OUTPUT);
  pinMode(STATUS_LED_RED, OUTPUT);

  digitalWrite(STATUS_LED_GREEN, LOW);
  digitalWrite(STATUS_LED_YELLOW, LOW);
  digitalWrite(STATUS_LED_RED, LOW);


  // ========================================================
  // DHT22
  // ========================================================

  dht.begin();


  // ========================================================
  // STATIC IP
  // ========================================================

  IPAddress local_IP(192, 168, 1, 20);
  IPAddress gateway(192, 168, 1, 1);
  IPAddress subnet(255, 255, 255, 0);
  IPAddress dns(192, 168, 1, 1);

  if (!WiFi.config(local_IP, gateway, subnet, dns)) {

    Serial.println("Static IP configuration failed!");

  } else {

    Serial.println("Static IP configured: 192.168.1.20");
  }


  // ========================================================
  // WI-FI
  // ========================================================

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("Connecting to Wi-Fi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();

  Serial.print("Connected. IP address: ");
  Serial.println(WiFi.localIP());

  Serial.print("MAC Address: ");
  Serial.println(WiFi.macAddress());

  Serial.println("Enter this IP in the app's setup screen.");


  // ========================================================
  // mDNS
  // ========================================================

  if (MDNS.begin(MDNS_HOSTNAME)) {

    Serial.print("mDNS responder started: http://");
    Serial.print(MDNS_HOSTNAME);
    Serial.println(".local/");

    MDNS.addService("http", "tcp", 80);

  } else {

    Serial.println("mDNS responder failed to start");
  }


  // ========================================================
  // SERVER ROUTES
  // ========================================================

  server.on("/", HTTP_GET, handleRoot);
  server.on("/status", HTTP_GET, handleStatus);

  // Only fan control is available.
  server.on("/toggle/fan", HTTP_POST, handleToggleFan);

  server.begin();

  Serial.println("HTTP server started");
}


// ==========================================================
// LOOP
// ==========================================================

void loop() {

  // Automatic Wi-Fi reconnection.
  checkWiFi();


  // Process app and browser requests.
  server.handleClient();

  MDNS.update();


  // Read sensors every 2 seconds.
  if (millis() - lastReadingTime < READING_INTERVAL) {
    return;
  }

  lastReadingTime = millis();


  // ========================================================
  // HUMIDITY AND TEMPERATURE
  // ========================================================

  lastHumid = dht.readHumidity();
  lastTempC = dht.readTemperature();

  if (isnan(lastHumid) || isnan(lastTempC)) {
    Serial.println("Failed to read from DHT22");
  }


  // ========================================================
  // DUST SENSOR
  // ========================================================

  lastDust = readDustDensity();


  // ========================================================
  // STATUS LEDs
  // ========================================================

  updateStatusLED(lastDust);


  // ========================================================
  // SERIAL OUTPUT
  // ========================================================

  Serial.print("Dust Density: ");
  Serial.print(lastDust, 2);
  Serial.println(" ug/m3");


  // ========================================================
  // GENERAL STATUS
  // ========================================================

  Serial.printf(
    "Fan:%s Temp:%.1fC Humidity:%.1f%% "
    "Dust:%.2f ug/m3 WiFi:%s\n",
    fanOn ? "ON" : "OFF",
    lastTempC,
    lastHumid,
    lastDust,
    WiFi.status() == WL_CONNECTED
      ? "Connected"
      : "Disconnected"
  );
}