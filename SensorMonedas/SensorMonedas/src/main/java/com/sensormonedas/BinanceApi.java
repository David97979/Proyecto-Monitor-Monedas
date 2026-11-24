
package com.sensormonedas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BinanceApi {

    private static final String URL_BASE =
            "https://api.binance.com/api/v3/ticker/price?symbol=";

    private final HttpClient cliente;
    private final ObjectMapper conversorJSON;

    public BinanceApi() {
        cliente = HttpClient.newHttpClient();
        conversorJSON = new ObjectMapper();
    }

    public double obtenerPrecio(String moneda) {

        try {
            String url = URL_BASE + moneda;

            HttpRequest solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() != 200) {
                throw new RuntimeException(
                        "Error de Binance: HTTP " + respuesta.statusCode()
                );
            }

            JsonNode datos = conversorJSON.readTree(respuesta.body());

            return datos.get("price").asDouble();

        } catch (Exception e) {
            System.out.println(
                    "Error consultando " + moneda + ": " + e.getMessage()
            );

            return -1;
        }
    }
}

