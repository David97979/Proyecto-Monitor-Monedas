
package com.sensormonedas;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class conexionMqtt {

    private static final String BROKER =
            "tcp://localhost:1883";

    private static final String CLIENTE_ID =
            "SensorMonedas";

    private MqttClient cliente;

    public void conectar() {

        try {
            cliente = new MqttClient(BROKER, CLIENTE_ID);

            MqttConnectOptions opciones =
                    new MqttConnectOptions();

            opciones.setCleanSession(true);
            opciones.setAutomaticReconnect(true);

            cliente.connect(opciones);

            System.out.println(
                    "Conectado correctamente a Mosquitto."
            );

        } catch (MqttException e) {
            System.out.println(
                    "Error al conectar con Mosquitto: "
                            + e.getMessage()
            );
        }
    }

    public void publicar(String topico, String mensaje) {

        try {
            if (cliente != null && cliente.isConnected()) {

                cliente.publish(
                        topico,
                        mensaje.getBytes(),
                        1,
                        false
                );

                System.out.println(
                        "Mensaje publicado en: " + topico
                );

            } else {
                System.out.println(
                        "No se puede publicar: MQTT no está conectado."
                );
            }

        } catch (MqttException e) {
            System.out.println(
                    "Error al publicar: " + e.getMessage()
            );
        }
    }

    public void desconectar() {

        try {
            if (cliente != null && cliente.isConnected()) {

                cliente.disconnect();
                cliente.close();

                System.out.println(
                        "Desconectado de Mosquitto."
                );
            }

        } catch (MqttException e) {
            System.out.println(
                    "Error al desconectar: " + e.getMessage()
            );
        }
    }
}
