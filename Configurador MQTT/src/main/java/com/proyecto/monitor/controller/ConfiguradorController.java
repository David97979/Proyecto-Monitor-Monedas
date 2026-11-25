package com.proyecto.monitor.controller;

import com.proyecto.monitor.model.Configuracion;
import com.proyecto.monitor.service.MqttPublisher;
import org.eclipse.paho.client.mqttv3.MqttException;

public class ConfiguradorController {
    private final MqttPublisher mqttPublisher = new MqttPublisher();

    public String enviarConfiguracion(
            String direccionBroker,
            String topico,
            String moneda1,
            String limite1Texto,
            String moneda2,
            String limite2Texto,
            String duracionTexto,
            String volumenTexto,
            String modoAlerta,
            String intervaloTexto) throws MqttException {

        if (direccionBroker == null || direccionBroker.isBlank()) {
            throw new IllegalArgumentException("Escribe la dirección del broker.");
        }
        if (topico == null || topico.isBlank()) {
            throw new IllegalArgumentException("Escribe el tópico MQTT.");
        }

        double limite1 = Double.parseDouble(limite1Texto.trim());
        double limite2 = Double.parseDouble(limite2Texto.trim());
        int duracion = Integer.parseInt(duracionTexto.trim());
        int volumen = Integer.parseInt(volumenTexto.trim());
        int intervalo = Integer.parseInt(intervaloTexto.trim());

        Configuracion configuracion = new Configuracion(
                moneda1,
                moneda2,
                limite1,
                limite2,
                duracion,
                volumen,
                modoAlerta,
                intervalo
        );

        mqttPublisher.publicarConfiguracion(
                direccionBroker.trim(),
                topico.trim(),
                configuracion
        );

        return "Configuración enviada correctamente al tópico " + topico.trim();
    }
}