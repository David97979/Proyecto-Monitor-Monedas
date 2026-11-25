package com.sensormonedas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

public class conexionMqtt {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENTE_ID = "SensorMonedas";

    private MqttClient cliente;
    private volatile int intervaloSeg = 30;
    private final ObjectMapper json = new ObjectMapper();

    public void conectar() {
        try {
            cliente = new MqttClient(BROKER, CLIENTE_ID);

            MqttConnectOptions opciones = new MqttConnectOptions();
            opciones.setCleanSession(true);
            opciones.setAutomaticReconnect(true);

            cliente.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable causa) {
                    System.out.println("Conexión MQTT perdida: " + causa.getMessage());
                }

                @Override
                public void messageArrived(String topico, MqttMessage mensaje) {
                    if (!"config/monitor01".equals(topico)) return;

                    try {
                        JsonNode datos = json.readTree(mensaje.getPayload());
                        JsonNode intervalo = datos.get("intervaloSeg");

                        if (intervalo != null) {
                            int nuevoIntervalo = intervalo.asInt();
                            if (nuevoIntervalo >= 5 && nuevoIntervalo <= 3600) {
                                intervaloSeg = nuevoIntervalo;
                                System.out.println(
                                        "Nuevo intervalo recibido: " + intervaloSeg + " segundos."
                                );
                            } else {
                                System.out.println("Intervalo recibido fuera del rango permitido.");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("No se pudo leer la configuración: " + e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // SensorMonedas no publica de forma asíncrona en este callback.
                }
            });

            cliente.connect(opciones);
            cliente.subscribe("config/monitor01", 1);

            System.out.println("Conectado a Mosquitto y suscrito a config/monitor01.");

        } catch (MqttException e) {
            System.out.println("Error MQTT: " + e.getMessage());
        }
    }

    public int getIntervaloSeg() {
        return intervaloSeg;
    }

    public void publicar(String topico, String mensaje) {
        try {
            if (cliente != null && cliente.isConnected()) {
                cliente.publish(topico, mensaje.getBytes(), 1, false);
                System.out.println("Publicado en " + topico + ": " + mensaje);
            } else {
                System.out.println("No se puede publicar: MQTT no está conectado.");
            }
        } catch (MqttException e) {
            System.out.println("Error al publicar: " + e.getMessage());
        }
    }

    public void desconectar() {
        try {
            if (cliente != null && cliente.isConnected()) {
                cliente.disconnect();
                cliente.close();
                System.out.println("Desconectado de Mosquitto.");
            }
        } catch (MqttException e) {
            System.out.println("Error al desconectar: " + e.getMessage());
        }
    }
}