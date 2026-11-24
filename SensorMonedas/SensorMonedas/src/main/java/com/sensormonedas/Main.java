
package com.sensormonedas;

public class Main {

    public static void main(String[] args) {

        BinanceApi binance = new BinanceApi();
        conexionMqtt mqtt = new conexionMqtt();

        System.out.println("--------------------------------");
        System.out.println("       SensorMonedas");
        System.out.println("--------------------------------");

        mqtt.conectar();

        while (true) {

            double precioBitcoin =
                    binance.obtenerPrecio("BTCUSDT");

            double precioSolana =
                    binance.obtenerPrecio("SOLUSDT");

            System.out.println();
            System.out.println("Actualizacion de precios:");

            System.out.println(
                    "Bitcoin (BTC): " + precioBitcoin + " USDT"
            );

            System.out.println(
                    "Solana (SOL): " + precioSolana + " USDT"
            );

            if (precioBitcoin >= 0) {
                mqtt.publicar(
                        "crypto/bitcoin",
                        String.valueOf(precioBitcoin)
                );
            }

            if (precioSolana >= 0) {
                mqtt.publicar(
                        "crypto/solana",
                        String.valueOf(precioSolana)
                );
            }

            System.out.println("--------------------------------");

            try {
                Thread.sleep(30000);

            } catch (InterruptedException e) {

                System.out.println(
                        "Actualizacion interrumpida."
                );

                Thread.currentThread().interrupt();
                break;
            }
        }

        mqtt.desconectar();
    }
}
