import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

public class Lab5Client {
    private JFrame frame = new JFrame("Chat Client");
    private JTextArea messageArea = new JTextArea(20, 50);
    private JTextField messageInput = new JTextField(50);
    private JTextField serverField = new JTextField("localhost", 10);
    private JTextField portField = new JTextField("5555", 5);
    private JButton sendButton = new JButton("Send");
    private JButton connectButton = new JButton("Connect");

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Lab5Client() {
        messageArea.setEditable(false);
        frame.add(new JScrollPane(messageArea), BorderLayout.NORTH);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JLabel("Server:"));
        bottomPanel.add(serverField);
        bottomPanel.add(new JLabel("Port:"));
        bottomPanel.add(portField);
        bottomPanel.add(connectButton);

        JPanel messagePanel = new JPanel();
        messagePanel.add(messageInput);
        messagePanel.add(sendButton);
        
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(messagePanel, BorderLayout.CENTER);

        sendButton.setEnabled(false);
        messageInput.setEnabled(false);


        connectButton.addActionListener(e -> connectToServer());
        sendButton.addActionListener(e -> sendMessage());

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket(serverField.getText(), Integer.parseInt(portField.getText()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), false);

            sendButton.setEnabled(true);
            connectButton.setEnabled(false);
            messageInput.setEnabled(true);

            Thread incomingReader = new Thread(new IncomingReader());
            incomingReader.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to connect to server.");
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        if (out != null && !messageInput.getText().isEmpty()) {
            out.println(messageInput.getText());
            out.flush();
            messageInput.setText("");
        }
    }

    private class IncomingReader implements Runnable {
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    messageArea.append(message + " \n");
                } 
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        Lab5Client client = new Lab5Client();
    }
}
