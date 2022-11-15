package com.networking.chatclient;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

class GraphicalInterface extends UserInterface {

    public GraphicalInterface(WebClient client) {
        super(client);
    }

    @Override
    public void run() {
        // Creating the Frame
        JFrame frame = new JFrame("Chat Frame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        createTextPanel();

        // Creating the panel at bottom and adding components
        JPanel panel = new JPanel(); // the panel is not visible in output
        JLabel label = new JLabel("Enter Text");
        JTextField tf = new JTextField(10); // accepts upto 10 characters
        JButton send = new JButton("Send");
        JButton reset = new JButton("Reset");
        panel.add(label); // Components Added using Flow Layout
        panel.add(tf);
        panel.add(send);
        panel.add(reset);

        // Text Area at the Center
        JTextArea ta = new JTextArea();

        // Adding Components to the frame.
        frame.getContentPane().add(BorderLayout.SOUTH, panel);
        frame.getContentPane().add(BorderLayout.CENTER, ta);
        frame.setVisible(true);

        send.addActionListener((arg0) -> {
            System.out.println(tf.getText());
        });
    }

    private void createTextPanel() {

    }

}