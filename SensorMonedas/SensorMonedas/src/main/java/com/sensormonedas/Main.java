package com.sensormonedas;

import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        BinanceApi binance = new BinanceApi();
        conexionMqtt mqtt = new conexionMqtt();

        String[] monedas = {"BTC", "SOL", "ETH", "XRP", "ADA", "DOGE"};

        System.out.println("SensorMonedas");
        mqtt.conectar();

        long ultimaConsulta = 0;

        while (!Thread.currentThread().isInterrupted()) {
            long ahora = System.currentTimeMillis();
            long intervaloMs = mqtt.getIntervaloSeg() * 1000L;

            if (ahora - ultimaConsulta >= intervaloMs) {
                System.out.println(
                        "Consultando precios. Intervalo actual: "
                                + mqtt.getIntervaloSeg() + " segundos."
                );

                for (String moneda : monedas) {
                    double precio = binance.obtenerPrecio(moneda + "USDT");

                    if (precio >= 0) {
                        String topico =
                                "crypto/" + moneda.toLowerCase(Locale.ROOT);

                        mqtt.publicar(topico, Double.toString(precio));
                        System.out.println(moneda + ": " + precio + " USDT");
                    }
                }

                ultimaConsulta = System.currentTimeMillis();
                System.out.println("------------------------------");
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        mqtt.desconectar();
    }
}