import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Lab5Client {
    private JFrame frame = new JFrame("Chat Client");
    private JTextArea messageArea = new JTextArea(20, 50);
    private JTextField messageInput = new JTextField(50);
    private JTextField serverField = new JTextField("localhost", 10);
    private JTextField portField = new JTextField("5555", 5);
    private JTextField usernameField = new JTextField(15);
    private JButton sendButton = new JButton("Send");
    private JButton connectButton = new JButton("Connect");

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean isConnected = false;

    private IncomingReader incomingReader;

    public Lab5Client() {
        messageArea.setEditable(false);
        
        // Main frame uses BorderLayout
        frame.setLayout(new BorderLayout());
        
        // Top panel for connection settings (GridLayout)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(new JLabel("Username:"));
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(usernameField);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(new JLabel("Server:"));
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(serverField);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(new JLabel("Port:"));
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(portField);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(connectButton);
        
        // Message area in center
        frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        
        // Bottom panel for message input (GridLayout)
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(messageInput);
        bottomPanel.add(sendButton);
        
        // Add panels to frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.setEnabled(false);
        messageInput.setEnabled(false);
        usernameField.setEnabled(true);

        connectButton.addActionListener(e -> handleConnect(e));
        sendButton.addActionListener(e -> sendMessage());

        // Add window closing listener
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectFromServer();
            }
        });

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void disconnectFromServer() {
        System.out.println("isConnected: " + isConnected);
        System.out.println("out: " + out);
        if (isConnected && out != null) {
            try {
                System.out.println("Disconnecting from server...");
                out.println("[System] " + username + " has left the chat");
                out.flush();
                if (incomingReader != null) {
                    incomingReader.stop();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                cleanupConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanupConnection() {
        isConnected = false;
        sendButton.setEnabled(false);
        messageInput.setEnabled(false);
        connectButton.setEnabled(true);
        usernameField.setEnabled(true);
        connectButton.setText("Connect");
        
        // Clear the streams
        in = null;
        out = null;
        socket = null;
        incomingReader = null;
    }

    private void handleConnect(ActionEvent e) {
        if (e.getActionCommand().equals("Connect")) {
            if (usernameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter a username first.");
                return;
            }
            
            try {
                username = usernameField.getText().trim();
                socket = new Socket(serverField.getText(), Integer.parseInt(portField.getText()));
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), false);

                sendButton.setEnabled(true);
                connectButton.setText("Disconnect");
                messageInput.setEnabled(true);
                usernameField.setEnabled(false);
                serverField.setEnabled(false);
                portField.setEnabled(false);
                isConnected = true;

                // broadcast username to server
                out.println("[System] " + username + " has joined the chat");
                out.flush();

                incomingReader = new IncomingReader();
                Thread readerThread = new Thread(incomingReader);
                readerThread.start();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to connect to server.");
                ex.printStackTrace();
            }
        } else if (e.getActionCommand().equals("Disconnect")) {
            disconnectFromServer();
        }
    }

    private void sendMessage() {
        if (out != null && !messageInput.getText().isEmpty()) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String message = "[" + time + "] " + username + ": " + messageInput.getText();
            out.println(message);
            out.flush();
            messageInput.setText("");
        }
    }

    private class IncomingReader implements Runnable {
        public void stop() {
            isConnected = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            String message;
            try {
                while (isConnected && (message = in.readLine()) != null) {
                    messageArea.append(message + "\n");
                } 
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanupConnection();
            }
        }
    }
    
    public static void main(String[] args) {
        Lab5Client client = new Lab5Client();
    }
}
