# Configurador del Monitor

Aplicación de escritorio Java Swing para enviar la configuración del monitor por MQTT.

## Datos predeterminados

- Broker: `tcp://localhost:1883` (SensorMonedas y Mosquitto en el mismo computador).
- Tópico: `config/monitor01`.
- Mensaje: JSON con `moneda1`, `moneda2`, `limite1`, `limite2` y `duracionAlarmaSeg`.
- QoS: 1; mensaje retenido para que el monitor pueda recibir la última configuración al suscribirse.

## Ejecución

Requiere JDK 17 o superior y Maven. Desde esta carpeta ejecuta:

```text
mvn clean package
java -jar target/configurador-1.0.0.jar
```

El broker debe estar ejecutándose. Si Mosquitto está en otra máquina, cambia el campo Broker por su dirección, por ejemplo `tcp://192.168.1.2:1883`.

La aplicación permite BTC y SOL porque SensorMonedas actualmente publica esas dos monedas. El ESP32 aún debe suscribirse a `config/monitor01` y procesar el JSON antes de que los cambios de esta aplicación afecten el dispositivo.
