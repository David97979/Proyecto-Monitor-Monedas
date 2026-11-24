package com.proyecto.monitor;

import com.proyecto.monitor.vista.ConfiguradorFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConfiguradorFrame ventana = new ConfiguradorFrame();
            ventana.setVisible(true);
        });
    }
}