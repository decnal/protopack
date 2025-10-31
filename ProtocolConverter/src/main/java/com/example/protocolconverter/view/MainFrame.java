package com.example.protocolconverter.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.example.protocolconverter.model.ProtocolConverter;

public class MainFrame extends JFrame {

    private JTextArea upperTextArea;
    private JTextArea lowerTextArea;
    private ProtocolConverter protocolConverter;
    private JRadioButton xmlOutputRadioButton;
    private JRadioButton jsonOutputRadioButton;
    private JRadioButton flatOutputRadioButton;
    private JRadioButton normalRadioButton;
    private JRadioButton condensedRadioButton;
    private JRadioButton easyReadRadioButton;

    public MainFrame() {
        protocolConverter = new ProtocolConverter();
        setTitle("Protocol Converter");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Add ActionListeners for menu items
        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        // Main Panel
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Upper Panel
        JPanel upperPanel = new JPanel(new BorderLayout(5, 5));
        upperTextArea = new JTextArea();
        JScrollPane upperScrollPane = new JScrollPane(upperTextArea);
        upperPanel.add(upperScrollPane, BorderLayout.CENTER);

        JPanel upperOptionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        upperOptionsPanel.setBorder(BorderFactory.createTitledBorder("Input"));
        JRadioButton autoRadioButton = new JRadioButton("Auto");
        JRadioButton xmlInputRadioButton = new JRadioButton("XML");
        JRadioButton jsonInputRadioButton = new JRadioButton("JSON");
        JRadioButton flatInputRadioButton = new JRadioButton("FLAT");
        ButtonGroup inputGroup = new ButtonGroup();
        inputGroup.add(autoRadioButton);
        inputGroup.add(xmlInputRadioButton);
        inputGroup.add(jsonInputRadioButton);
        inputGroup.add(flatInputRadioButton);
        upperOptionsPanel.add(autoRadioButton);
        upperOptionsPanel.add(xmlInputRadioButton);
        upperOptionsPanel.add(jsonInputRadioButton);
        upperOptionsPanel.add(flatInputRadioButton);
        upperPanel.add(upperOptionsPanel, BorderLayout.EAST);

        // Lower Panel
        JPanel lowerPanel = new JPanel(new BorderLayout(5, 5));
        lowerTextArea = new JTextArea();
        JScrollPane lowerScrollPane = new JScrollPane(lowerTextArea);
        lowerPanel.add(lowerScrollPane, BorderLayout.CENTER);

        JPanel lowerOptionsPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel outputTypePanel = new JPanel(new GridLayout(3, 1, 5, 5));
        outputTypePanel.setBorder(BorderFactory.createTitledBorder("Output"));
        xmlOutputRadioButton = new JRadioButton("XML");
        jsonOutputRadioButton = new JRadioButton("JSON");
        flatOutputRadioButton = new JRadioButton("FLAT");
        ButtonGroup outputGroup = new ButtonGroup();
        outputGroup.add(xmlOutputRadioButton);
        outputGroup.add(jsonOutputRadioButton);
        outputGroup.add(flatOutputRadioButton);
        outputTypePanel.add(xmlOutputRadioButton);
        outputTypePanel.add(jsonOutputRadioButton);
        outputTypePanel.add(flatOutputRadioButton);

        JPanel densityPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        densityPanel.setBorder(BorderFactory.createTitledBorder("Density"));
        normalRadioButton = new JRadioButton("Normal");
        condensedRadioButton = new JRadioButton("Condensed");
        easyReadRadioButton = new JRadioButton("Easy Read");
        ButtonGroup densityGroup = new ButtonGroup();
        densityGroup.add(normalRadioButton);
        densityGroup.add(condensedRadioButton);
        densityGroup.add(easyReadRadioButton);
        densityPanel.add(normalRadioButton);
        densityPanel.add(condensedRadioButton);
        densityPanel.add(easyReadRadioButton);

        ActionListener conversionListener = e -> convertText();
        xmlOutputRadioButton.addActionListener(conversionListener);
        jsonOutputRadioButton.addActionListener(conversionListener);
        flatOutputRadioButton.addActionListener(conversionListener);
        normalRadioButton.addActionListener(conversionListener);
        condensedRadioButton.addActionListener(conversionListener);
        easyReadRadioButton.addActionListener(conversionListener);

        lowerOptionsPanel.add(outputTypePanel);
        lowerOptionsPanel.add(densityPanel);
        lowerPanel.add(lowerOptionsPanel, BorderLayout.EAST);

        mainPanel.add(upperPanel);
        mainPanel.add(lowerPanel);

        add(mainPanel);

        // Add DocumentListener to upperTextArea
        upperTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLowerTextArea();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLowerTextArea();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text components do not fire these events
            }
        });
    }

    private void updateLowerTextArea() {
        convertText();
    }

    private void convertText() {
        String input = upperTextArea.getText();
        String output;

        if (xmlOutputRadioButton.isSelected()) {
            output = protocolConverter.convertToXML(input);
        } else if (jsonOutputRadioButton.isSelected()) {
            output = protocolConverter.convertToJson(input);
        } else if (flatOutputRadioButton.isSelected()) {
            output = protocolConverter.convertToFlat(input);
        } else {
            output = input; // Default to no conversion if nothing is selected
        }

        // Placeholder for density logic
        if (condensedRadioButton.isSelected()) {
            output = output.replaceAll("\\s+", "");
        } else if (easyReadRadioButton.isSelected()) {
            // This is a placeholder, real "easy read" would involve more complex formatting
            output = output.replaceAll("(.{80})", "$1\n");
        }

        lowerTextArea.setText(output);
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileReader reader = new FileReader(file)) {
                upperTextArea.read(reader, null);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                lowerTextArea.write(writer);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
