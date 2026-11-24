package com.proyecto.monitor.vista;
import com.proyecto.monitor.controller.ConfiguradorController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ConfiguradorFrame extends JFrame {
    private final JTextField brokerField =
            new JTextField("tcp://localhost:1883");
    private final JTextField topicField =
            new JTextField("config/monitor01");

    private final JComboBox<String> moneda1Field =
            new JComboBox<>(new String[]{"BTC", "SOL"});
    private final JComboBox<String> moneda2Field =
            new JComboBox<>(new String[]{"SOL", "BTC"});

    private final JTextField limite1Field =
            new JTextField("95000");
    private final JTextField limite2Field =
            new JTextField("200");
    private final JSpinner duracionField =
            new JSpinner(new SpinnerNumberModel(2, 1, 3600, 1));

    private final JLabel estadoLabel =
            new JLabel("Listo para enviar configuración.");

    private final ConfiguradorController controller =
            new ConfiguradorController();

    public ConfiguradorFrame() {
        setTitle("Configurador del monitor de criptomonedas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);

        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(new EmptyBorder(18, 20, 18, 20));
        setContentPane(contenido);

        JLabel titulo = new JLabel("Configuración del Monitor");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        contenido.add(titulo, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 10, 12));

        formulario.add(new JLabel("Broker MQTT:"));
        formulario.add(brokerField);

        formulario.add(new JLabel("Tópico:"));
        formulario.add(topicField);

        formulario.add(new JLabel("Moneda del botón 1:"));
        formulario.add(moneda1Field);

        formulario.add(new JLabel("Límite de alarma moneda 1:"));
        formulario.add(limite1Field);

        formulario.add(new JLabel("Moneda del botón 2:"));
        formulario.add(moneda2Field);

        formulario.add(new JLabel("Límite de alarma moneda 2:"));
        formulario.add(limite2Field);

        formulario.add(new JLabel("Duración de alarma (segundos):"));
        formulario.add(duracionField);

        contenido.add(formulario, BorderLayout.CENTER);

        JButton enviarButton = new JButton("Enviar configuración");
        enviarButton.addActionListener(event -> enviarConfiguracion());

        JPanel inferior = new JPanel(new BorderLayout(8, 8));
        inferior.add(enviarButton, BorderLayout.NORTH);
        inferior.add(estadoLabel, BorderLayout.SOUTH);
        contenido.add(inferior, BorderLayout.SOUTH);
    }

    private void enviarConfiguracion() {
        try {
            String resultado = controller.enviarConfiguracion(
                    brokerField.getText(),
                    topicField.getText(),
                    (String) moneda1Field.getSelectedItem(),
                    limite1Field.getText(),
                    (String) moneda2Field.getSelectedItem(),
                    limite2Field.getText(),
                    duracionField.getValue().toString()
            );

            estadoLabel.setText(resultado);
            JOptionPane.showMessageDialog(
                    this,
                    resultado,
                    "Envío exitoso",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception error) {
            estadoLabel.setText("No se pudo enviar la configuración.");
            JOptionPane.showMessageDialog(
                    this,
                    error.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}