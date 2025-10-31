package com.example.protocolconverter;

import javax.swing.SwingUtilities;

import com.example.protocolconverter.view.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
