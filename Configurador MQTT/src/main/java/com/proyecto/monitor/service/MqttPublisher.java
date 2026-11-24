package com.proyecto.monitor.service;
import com.proyecto.monitor.model.Configuracion;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MqttPublisher {

    public void publicarConfiguracion(
            String direccionBroker,
            String topico,
            Configuracion configuracion) throws MqttException {

        String mensajeJson = String.format(
                Locale.US,
                "{\"moneda1\":\"%s\",\"moneda2\":\"%s\",\"limite1\":%.8f,\"limite2\":%.8f,\"duracionAlarmaSeg\":%d}",
                configuracion.getMoneda1(),
                configuracion.getMoneda2(),
                configuracion.getLimite1(),
                configuracion.getLimite2(),
                configuracion.getDuracionAlarmaSeg()
        );

        MqttClient cliente = new MqttClient(
                direccionBroker,
                MqttClient.generateClientId()
        );

        try {
            MqttConnectOptions opciones = new MqttConnectOptions();
            opciones.setCleanSession(true);
            opciones.setConnectionTimeout(5);

            cliente.connect(opciones);

            MqttMessage mensaje = new MqttMessage(
                    mensajeJson.getBytes(StandardCharsets.UTF_8)
            );
            mensaje.setQos(1);
            mensaje.setRetained(true);

            cliente.publish(topico, mensaje);
        } finally {
            if (cliente.isConnected()) {
                cliente.disconnect();
            }
            cliente.close();
        }
    }
}