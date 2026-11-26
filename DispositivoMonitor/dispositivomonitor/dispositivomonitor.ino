#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// --- Pines exactos según tu placa ESP32 ---
// Bus I2C 0 (LCD 16x2): SDA = D21, SCL = D22
// Bus I2C 1 (OLED 0.96"): SDA = D32, SCL = D33
#define OLED_SDA    32  // D32
#define OLED_SCL    33  // D33

#define BTN1_PIN    18  // D18 -> Selección Moneda 1
#define BTN2_PIN    19  // D19 -> Selección Moneda 2
#define LED_VERDE   25  // D25 -> Sube el precio
#define LED_ROJO    26  // D26 -> Baja el precio
#define LED_BLANCO  27  // D27 -> Precio igual / estable
#define AUDIO_PIN   14  // D14 -> Entrada A+ del amplificador PAM8302A

LiquidCrystal_I2C lcd(0x27, 16, 2);
TwoWire I2C_OLED = TwoWire(1);
Adafruit_SSD1306 oled(128, 64, &I2C_OLED, -1);

// --- Configuración Wi-Fi y Broker Mosquitto Local ---
const char* WIFI_SSID     = "Wifi";
const char* WIFI_PASSWORD = "clave wifi";
const char* MQTT_SERVER   = "192.168.x.x";
const uint16_t MQTT_PORT  = 1883;
const char* TOPICO_CONFIG = "config/monitor01";

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
unsigned long ultimoIntentoMQTT = 0;

// --- Parámetros configurables desde MQTT ---
String moneda1 = "BTC";
String moneda2 = "SOL";
int monedaActiva = 1;

float precio1 = 0.0, precioAnt1 = 0.0;
float precio2 = 0.0, precioAnt2 = 0.0;

float limite1 = 95000.0;
float limite2 = 200.0;
int duracionAlarmaSeg = 2;
int volumenAlarma = 80;       // 0 a 100%
String modoAlerta = "MAX";    // "MAX" o "MIN"
int intervaloSeg = 30;        // Informativo / sincronizado con config

// --- Historial de precios para la gráfica de la OLED ---
const int MAX_PUNTOS = 20;
float historial1[MAX_PUNTOS];
float historial2[MAX_PUNTOS];
int cantPuntos1 = 0;
int cantPuntos2 = 0;

unsigned long ultimoPulsoBoton = 0;
bool alarmaActiva = false;
unsigned long inicioAlarma = 0;
unsigned long ultimoCambioNota = 0;
int pasoMelodia = 0;

const int notasAlarma[] = {523, 659, 784, 1047};

String topicoMoneda(const String &simbolo) {
  String s = simbolo;
  s.trim();
  s.toLowerCase();
  return "crypto/" + s;
}

void tocarFrecuencia(int freq, int volPct) {
  int duty = map(constrain(volPct, 0, 100), 0, 100, 0, 127);

  #if ESP_ARDUINO_VERSION >= ESP_ARDUINO_VERSION_VAL(3, 0, 0)
    if (freq > 0 && duty > 0) {
      ledcChangeFrequency(AUDIO_PIN, freq, 8);
      ledcWrite(AUDIO_PIN, duty);
    } else {
      ledcWrite(AUDIO_PIN, 0);
    }
  #else
    if (freq > 0 && duty > 0) {
      ledcChangeFrequency(0, freq, 8);
      ledcWrite(0, duty);
    } else {
      ledcWrite(0, 0);
    }
  #endif
}

void agregarHistorial(float arreglo[], int &cantidad, float nuevoValor) {
  if (cantidad < MAX_PUNTOS) {
    arreglo[cantidad] = nuevoValor;
    cantidad++;
  } else {
    for (int i = 0; i < MAX_PUNTOS - 1; i++) {
      arreglo[i] = arreglo[i + 1];
    }
    arreglo[MAX_PUNTOS - 1] = nuevoValor;
  }
}

void apagarLEDs() {
  digitalWrite(LED_VERDE, LOW);
  digitalWrite(LED_ROJO, LOW);
  digitalWrite(LED_BLANCO, LOW);
}

void actualizarLEDs(float actual, float anterior) {
  apagarLEDs();
  if (actual <= 0.0) return;

  if (anterior == 0.0 || actual == anterior) {
    digitalWrite(LED_BLANCO, HIGH);
  } else if (actual > anterior) {
    digitalWrite(LED_VERDE, HIGH);
  } else {
    digitalWrite(LED_ROJO, HIGH);
  }
}

bool cumpleCondicionAlarma(float actual, float anterior, float limite) {
  if (actual <= 0.0 || limite <= 0.0) return false;
  if (modoAlerta == "MIN") {
    return (actual <= limite) && (anterior <= 0.0 || anterior > limite);
  }
  // Por defecto: "MAX"
  return (actual >= limite) && (anterior < limite);
}

bool estadoEnAlarma(float actual, float limite) {
  if (actual <= 0.0 || limite <= 0.0) return false;
  if (modoAlerta == "MIN") return actual <= limite;
  return actual >= limite;
}

void activarAlarma() {
  if (volumenAlarma <= 0) return;
  alarmaActiva = true;
  inicioAlarma = millis();
  ultimoCambioNota = millis();
  pasoMelodia = 0;
  tocarFrecuencia(notasAlarma[0], volumenAlarma);
}

void apagarAlarma() {
  alarmaActiva = false;
  tocarFrecuencia(0, 0);
}

void actualizarOLED() {
  oled.clearDisplay();
  oled.setTextColor(SSD1306_WHITE);

  String nombre  = (monedaActiva == 1) ? moneda1 : moneda2;
  float actual   = (monedaActiva == 1) ? precio1 : precio2;
  float anterior = (monedaActiva == 1) ? precioAnt1 : precioAnt2;
  float limite   = (monedaActiva == 1) ? limite1 : limite2;
  float* hist    = (monedaActiva == 1) ? historial1 : historial2;
  int puntos     = (monedaActiva == 1) ? cantPuntos1 : cantPuntos2;

  oled.setTextSize(1);
  oled.setCursor(0, 0);
  oled.print(nombre + " [" + modoAlerta + "]");

  float varPct = 0.0;
  if (anterior > 0.0 && actual > 0.0) {
    varPct = ((actual - anterior) / anterior) * 100.0;
  }
  oled.setCursor(82, 0);
  if (varPct >= 0) oled.print("+");
  oled.print(String(varPct, 2) + "%");

  oled.drawLine(0, 10, 127, 10, SSD1306_WHITE);

  oled.drawRect(0, 13, 128, 37, SSD1306_WHITE);
  if (puntos >= 2) {
    float minVal = hist[0], maxVal = hist[0];
    for (int i = 1; i < puntos; i++) {
      if (hist[i] < minVal) minVal = hist[i];
      if (hist[i] > maxVal) maxVal = hist[i];
    }
    if (maxVal == minVal) {
      maxVal += 1.0;
      minVal -= 1.0;
    }

    int pasoX = 122 / (MAX_PUNTOS - 1);
    for (int i = 0; i < puntos - 1; i++) {
      int x1 = 3 + (i * pasoX);
      int y1 = 47 - (int)(((hist[i] - minVal) / (maxVal - minVal)) * 31.0);
      int x2 = 3 + ((i + 1) * pasoX);
      int y2 = 47 - (int)(((hist[i + 1] - minVal) / (maxVal - minVal)) * 31.0);
      oled.drawLine(x1, y1, x2, y2, SSD1306_WHITE);
      oled.fillCircle(x2, y2, 1, SSD1306_WHITE);
    }
  } else {
    oled.setCursor(18, 28);
    oled.print("Recopilando datos");
  }

  oled.setCursor(0, 54);
  oled.print("ALARMA:");
  int porcentaje = (limite > 0) ? (int)((actual / limite) * 100.0) : 0;
  if (porcentaje > 100) porcentaje = 100;
  if (porcentaje < 0) porcentaje = 0;

  oled.drawRect(45, 53, 82, 10, SSD1306_WHITE);
  int anchoBarra = map(porcentaje, 0, 100, 0, 78);
  oled.fillRect(47, 55, anchoBarra, 6, SSD1306_WHITE);

  oled.invertDisplay(estadoEnAlarma(actual, limite));
  oled.display();
}

void actualizarPantallas() {
  lcd.clear();
  String nombre = (monedaActiva == 1) ? moneda1 : moneda2;
  float precio  = (monedaActiva == 1) ? precio1 : precio2;
  float limite  = (monedaActiva == 1) ? limite1 : limite2;

  lcd.setCursor(0, 0);
  if (precio > 0) {
    lcd.print(nombre + ": $" + String(precio, 2));
  } else {
    lcd.print(nombre + ": Esperando...");
  }

  lcd.setCursor(0, 1);
  lcd.print("L:$" + String(limite, 2) + " " + modoAlerta);

  actualizarOLED();
}

void suscribirMonedas() {
  String t1 = topicoMoneda(moneda1);
  String t2 = topicoMoneda(moneda2);
  mqttClient.subscribe(t1.c_str());
  mqttClient.subscribe(t2.c_str());
  Serial.println("Suscrito a: " + t1 + " y " + t2);
}

void aplicarConfiguracionJSON(const String &jsonTexto) {
  StaticJsonDocument<384> doc;
  DeserializationError err = deserializeJson(doc, jsonTexto);
  if (err) {
    Serial.print("Error parseando JSON de config: ");
    Serial.println(err.c_str());
    return;
  }

  String tAnterior1 = topicoMoneda(moneda1);
  String tAnterior2 = topicoMoneda(moneda2);

  String nuevaMoneda1 = doc.containsKey("moneda1") ? String(doc["moneda1"].as<const char*>()) : moneda1;
  String nuevaMoneda2 = doc.containsKey("moneda2") ? String(doc["moneda2"].as<const char*>()) : moneda2;
  nuevaMoneda1.trim(); nuevaMoneda1.toUpperCase();
  nuevaMoneda2.trim(); nuevaMoneda2.toUpperCase();

  bool cambioMonedas = (nuevaMoneda1 != moneda1) || (nuevaMoneda2 != moneda2);

  if (cambioMonedas) {
    mqttClient.unsubscribe(tAnterior1.c_str());
    mqttClient.unsubscribe(tAnterior2.c_str());
    moneda1 = nuevaMoneda1;
    moneda2 = nuevaMoneda2;

    precio1 = precioAnt1 = 0.0;
    precio2 = precioAnt2 = 0.0;
    cantPuntos1 = 0;
    cantPuntos2 = 0;
    apagarLEDs();
    suscribirMonedas();
  }

  if (doc.containsKey("limite1")) {
    float l1 = doc["limite1"].as<float>();
    if (l1 > 0) limite1 = l1;
  }
  if (doc.containsKey("limite2")) {
    float l2 = doc["limite2"].as<float>();
    if (l2 > 0) limite2 = l2;
  }
  if (doc.containsKey("duracionAlarmaSeg")) {
    int d = doc["duracionAlarmaSeg"].as<int>();
    if (d > 0) duracionAlarmaSeg = d;
  }
  if (doc.containsKey("volumenAlarma")) {
    volumenAlarma = constrain(doc["volumenAlarma"].as<int>(), 0, 100);
    if (volumenAlarma == 0) apagarAlarma();
  }
  if (doc.containsKey("modoAlerta")) {
    String modo = String(doc["modoAlerta"].as<const char*>());
    modo.trim(); modo.toUpperCase();
    if (modo == "MAX" || modo == "MIN") {
      modoAlerta = modo;
    }
  }
  if (doc.containsKey("intervaloSeg")) {
    int iv = doc["intervaloSeg"].as<int>();
    if (iv >= 5) intervaloSeg = iv;
  }

  float actualActiva = (monedaActiva == 1) ? precio1 : precio2;
  float antActiva    = (monedaActiva == 1) ? precioAnt1 : precioAnt2;
  actualizarLEDs(actualActiva, antActiva);
  actualizarPantallas();

  Serial.println("Configuración aplicada correctamente.");
}

void actualizarPrecio(int moneda, float nuevoPrecio) {
  if (!isfinite(nuevoPrecio) || nuevoPrecio <= 0) return;

  float anterior = (moneda == 1) ? precio1 : precio2;
  float limite   = (moneda == 1) ? limite1 : limite2;

  if (moneda == 1) {
    precioAnt1 = precio1;
    precio1 = nuevoPrecio;
    agregarHistorial(historial1, cantPuntos1, nuevoPrecio);
  } else {
    precioAnt2 = precio2;
    precio2 = nuevoPrecio;
    agregarHistorial(historial2, cantPuntos2, nuevoPrecio);
  }

  if (cumpleCondicionAlarma(nuevoPrecio, anterior, limite)) {
    activarAlarma();
  }

  if (moneda == monedaActiva) {
    actualizarLEDs(nuevoPrecio, anterior);
    actualizarPantallas();
  }

  String nombre = (moneda == 1) ? moneda1 : moneda2;
  Serial.printf("Precio %s recibido: %.4f\n", nombre.c_str(), nuevoPrecio);
}

void recibirMQTT(char* topico, byte* payload, unsigned int longitud) {
  String mensaje;
  mensaje.reserve(longitud + 1);
  for (unsigned int i = 0; i < longitud; i++) {
    mensaje += (char)payload[i];
  }
  mensaje.trim();

  if (strcmp(topico, TOPICO_CONFIG) == 0) {
    aplicarConfiguracionJSON(mensaje);
    return;
  }

  float precio = mensaje.toFloat();
  if (strcmp(topico, topicoMoneda(moneda1).c_str()) == 0) {
    actualizarPrecio(1, precio);
  } else if (strcmp(topico, topicoMoneda(moneda2).c_str()) == 0) {
    actualizarPrecio(2, precio);
  }
}

void conectarMQTT() {
  if (mqttClient.connected()) return;
  if (millis() - ultimoIntentoMQTT < 3000) return;
  ultimoIntentoMQTT = millis();

  String clientId = "MonitorESP32-" + String((uint32_t)ESP.getEfuseMac(), HEX);
  Serial.print("Conectando al broker MQTT...");
  if (mqttClient.connect(clientId.c_str())) {
    Serial.println(" conectado");
    mqttClient.subscribe(TOPICO_CONFIG);
    suscribirMonedas();
    Serial.println("Escuchando " + String(TOPICO_CONFIG) + " y monedas activas.");
  } else {
    Serial.print(" fallo, estado MQTT: ");
    Serial.println(mqttClient.state());
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(BTN1_PIN, INPUT_PULLUP);
  pinMode(BTN2_PIN, INPUT_PULLUP);
  pinMode(LED_VERDE, OUTPUT);
  pinMode(LED_ROJO, OUTPUT);
  pinMode(LED_BLANCO, OUTPUT);

  #if ESP_ARDUINO_VERSION >= ESP_ARDUINO_VERSION_VAL(3, 0, 0)
    ledcAttach(AUDIO_PIN, 2000, 8);
  #else
    ledcSetup(0, 2000, 8);
    ledcAttachPin(AUDIO_PIN, 0);
  #endif
  tocarFrecuencia(0, 0);

  Wire.begin(21, 22);
  I2C_OLED.begin(OLED_SDA, OLED_SCL);

  lcd.init();
  lcd.backlight();

  if (oled.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    oled.clearDisplay();
    oled.setTextSize(1);
    oled.setTextColor(SSD1306_WHITE);
    oled.setCursor(15, 20);
    oled.println("MONITOR CRYPTO");
    oled.setCursor(25, 36);
    oled.println(" \(^o^)/ \(^o^)/ ");
    oled.display();
  }

  lcd.setCursor(0, 0);
  lcd.print("Conectando WiFi");
  lcd.setCursor(0, 1);
  lcd.print("Espere...");

  digitalWrite(LED_VERDE, HIGH);  tocarFrecuencia(523, volumenAlarma); delay(150);
  digitalWrite(LED_ROJO, HIGH);   tocarFrecuencia(659, volumenAlarma); delay(150);
  digitalWrite(LED_BLANCO, HIGH); tocarFrecuencia(784, volumenAlarma); delay(200);
  tocarFrecuencia(0, 0);
  apagarLEDs();

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Conectando al Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("Wi-Fi conectado. IP del ESP32: ");
  Serial.println(WiFi.localIP());

  mqttClient.setServer(MQTT_SERVER, MQTT_PORT);
  mqttClient.setCallback(recibirMQTT);
  mqttClient.setBufferSize(512);

  actualizarPantallas();
}

void loop() {
  // 1. Lectura de botones físicos (incluye atajo de volumen presionando ambos)
  if (millis() - ultimoPulsoBoton > 300) {
    bool b1 = (digitalRead(BTN1_PIN) == LOW);
    bool b2 = (digitalRead(BTN2_PIN) == LOW);

    if (b1 && b2) {
      if (volumenAlarma > 50) volumenAlarma = 35;
      else if (volumenAlarma > 0) volumenAlarma = 0;
      else volumenAlarma = 100;

      if (volumenAlarma > 0) {
        tocarFrecuencia(880, volumenAlarma);
        delay(100);
        tocarFrecuencia(0, 0);
      } else {
        apagarAlarma();
      }
      actualizarPantallas();
      Serial.println("Volumen cambiado a: " + String(volumenAlarma) + "%");
      ultimoPulsoBoton = millis();
    } else if (b1 && monedaActiva != 1) {
      monedaActiva = 1;
      actualizarLEDs(precio1, precioAnt1);
      actualizarPantallas();
      ultimoPulsoBoton = millis();
    } else if (b2 && monedaActiva != 2) {
      monedaActiva = 2;
      actualizarLEDs(precio2, precioAnt2);
      actualizarPantallas();
      ultimoPulsoBoton = millis();
    }
  }

  // 2. Mantener MQTT activo
  if (WiFi.status() == WL_CONNECTED) {
    if (!mqttClient.connected()) conectarMQTT();
    mqttClient.loop();
  }

  // 3. Melodía de alarma con el volumen configurado
  if (alarmaActiva) {
    if (millis() - inicioAlarma >= (unsigned long)duracionAlarmaSeg * 1000) {
      apagarAlarma();
    } else if (millis() - ultimoCambioNota >= 150) {
      pasoMelodia = (pasoMelodia + 1) % 4;
      tocarFrecuencia(notasAlarma[pasoMelodia], volumenAlarma);
      ultimoCambioNota = millis();
    }
  }
}