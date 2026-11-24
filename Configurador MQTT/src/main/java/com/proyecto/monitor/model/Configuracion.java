package com.proyecto.monitor.model;

public class Configuracion {
    private final String moneda1;
    private final String moneda2;
    private final double limite1;
    private final double limite2;
    private final int duracionAlarmaSeg;

    public Configuracion(String moneda1, String moneda2, double limite1, double limite2, int duracionAlarmaSeg) {

        if (moneda1 == null || moneda2 == null || moneda1.equals(moneda2)) {
            throw new IllegalArgumentException("Selecciona dos monedas distintas.");
        }
        if (limite1 <= 0 || limite2 <= 0) {
            throw new IllegalArgumentException("Los límites deben ser positivos.");
        }
        if (duracionAlarmaSeg <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor que cero.");
        }

        this.moneda1 = moneda1;
        this.moneda2 = moneda2;
        this.limite1 = limite1;
        this.limite2 = limite2;
        this.duracionAlarmaSeg = duracionAlarmaSeg;
    }

    public String getMoneda1() {
        return moneda1;
    }

    public String getMoneda2() {
        return moneda2;
    }

    public double getLimite1() {
        return limite1;
    }

    public double getLimite2() {
        return limite2;
    }

    public int getDuracionAlarmaSeg() {
        return duracionAlarmaSeg;
    }
}