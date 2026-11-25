package com.proyecto.monitor.model;

public class Configuracion {
    private final String moneda1;
    private final String moneda2;
    private final double limite1;
    private final double limite2;
    private final int duracionAlarmaSeg;
    private final int volumenAlarma;
    private final String modoAlerta;
    private final int intervaloSeg;

    public Configuracion(
            String moneda1,
            String moneda2,
            double limite1,
            double limite2,
            int duracionAlarmaSeg,
            int volumenAlarma,
            String modoAlerta,
            int intervaloSeg) {

        if (moneda1 == null || moneda2 == null || moneda1.equals(moneda2)) {
            throw new IllegalArgumentException("Selecciona dos monedas distintas.");
        }
        if (limite1 <= 0 || limite2 <= 0) {
            throw new IllegalArgumentException("Los límites deben ser positivos.");
        }
        if (duracionAlarmaSeg <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor que cero.");
        }
        if (volumenAlarma < 0 || volumenAlarma > 100) {
            throw new IllegalArgumentException("El volumen debe estar entre 0 y 100.");
        }
        if (!"MAX".equals(modoAlerta) && !"MIN".equals(modoAlerta)) {
            throw new IllegalArgumentException("El modo debe ser MAX o MIN.");
        }
        if (intervaloSeg < 5) {
            throw new IllegalArgumentException("El intervalo mínimo es de 5 segundos.");
        }

        this.moneda1 = moneda1;
        this.moneda2 = moneda2;
        this.limite1 = limite1;
        this.limite2 = limite2;
        this.duracionAlarmaSeg = duracionAlarmaSeg;
        this.volumenAlarma = volumenAlarma;
        this.modoAlerta = modoAlerta;
        this.intervaloSeg = intervaloSeg;
    }

    public String getMoneda1() { return moneda1; }
    public String getMoneda2() { return moneda2; }
    public double getLimite1() { return limite1; }
    public double getLimite2() { return limite2; }
    public int getDuracionAlarmaSeg() { return duracionAlarmaSeg; }
    public int getVolumenAlarma() { return volumenAlarma; }
    public String getModoAlerta() { return modoAlerta; }
    public int getIntervaloSeg() { return intervaloSeg; }
}