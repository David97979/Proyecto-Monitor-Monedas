package com.proyecto.monitor.controller;
import com.proyecto.monitor.model.Configuracion;
import com.proyecto.monitor.service.MqttPublisher;
import org.eclipse.paho.client.mqttv3.MqttException;

public class ConfiguradorController {
    private final MqttPublisher mqttPublisher;

    public ConfiguradorController() {
        this.mqttPublisher = new MqttPublisher();
    }

    public String enviarConfiguracion(
            String direccionBroker,
            String topico,
            String moneda1,
            String limite1Texto,
            String moneda2,
            String limite2Texto,
            String duracionTexto) throws Exception {

        if (direccionBroker == null || direccionBroker.isBlank()) {
            throw new IllegalArgumentException("Escribe la dirección del broker.");
        }
        if (topico == null || topico.isBlank()) {
            throw new IllegalArgumentException("Escribe el tópico MQTT.");
        }
        if (moneda1 == null || moneda2 == null || moneda1.equals(moneda2)) {
            throw new IllegalArgumentException("Selecciona dos monedas distintas.");
        }

        double limite1 = Double.parseDouble(limite1Texto.trim());
        double limite2 = Double.parseDouble(limite2Texto.trim());
        int duracion = Integer.parseInt(duracionTexto.trim());

        Configuracion configuracion =
                new Configuracion(moneda1, moneda2, limite1, limite2, duracion);

        try {
            mqttPublisher.publicarConfiguracion(
                    direccionBroker.trim(),
                    topico.trim(),
                    configuracion
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "Configuración enviada correctamente al tópico " + topico.trim();
    }
}