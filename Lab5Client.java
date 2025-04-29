import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class Lab5Client {
    private JFrame frame = new JFrame("Chat Client");
    private JTextArea messageArea = new JTextArea(20, 50);
    private JTextField messageInput = new JTextField(50);
    private JTextField serverField = new JTextField("localhost", 10);
    private JTextField portField = new JTextField("5555", 5);
    private JTextField usernameField = new JTextField(15);
    private JButton sendButton = new JButton("Send");
    private JButton connectButton = new JButton("Connect");
    private JLabel statusLabel = new JLabel("Disconnected");
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);
    private JPanel sidePanel;
    private JLabel typingLabel = new JLabel(" ");
    private Timer typingTimer;
    private static final int TYPING_TIMEOUT = 3000; // 3 seconds
    private boolean isTyping = false;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean isConnected = false;

    private IncomingReader incomingReader;

    public Lab5Client() {
        // Set up the main frame
        frame.setLayout(new BorderLayout());
        frame.setBackground(new Color(240, 240, 240));
        
        // Configure message area
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(new Color(255, 255, 255));
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Configure input field
        messageInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Configure buttons
        styleButton(sendButton);
        styleButton(connectButton);
        
        // Configure text fields
        styleTextField(usernameField);
        styleTextField(serverField);
        styleTextField(portField);
        
        // Status label styling
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Top panel for connection settings
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBackground(new Color(240, 240, 240));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add components to top panel
        topPanel.add(createLabeledField("Username:", usernameField));
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(createLabeledField("Server:", serverField));
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(createLabeledField("Port:", portField));
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(connectButton);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(statusLabel);
        
        // Message area in center with scroll pane
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(240, 240, 240));
        
        // Bottom panel for message input
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(new Color(240, 240, 240));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        
        // Configure typing label
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        typingLabel.setForeground(new Color(100, 100, 100));
        typingLabel.setHorizontalAlignment(SwingConstants.LEFT);
        typingLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        // Add typing label to bottom panel
        bottomPanel.add(typingLabel, BorderLayout.SOUTH);

        // Setup typing timer
        typingTimer = new Timer(TYPING_TIMEOUT, e -> {
            if (isTyping && isConnected && out != null) {
                out.println("[STOP_TYPING]");
                out.flush();
                isTyping = false;
            }
        });
        typingTimer.setRepeats(false);

        // Add typing listener to message input
        messageInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isTyping && isConnected && out != null) {
                    out.println("[TYPING]");
                    out.flush();
                    isTyping = true;
                }
                typingTimer.restart();
            }
        });
        
        // Configure user list
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userList.setBackground(new Color(250, 250, 250));
        userList.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Online Users"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Create side panel for user list
        sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(150, 0));
        sidePanel.setBackground(new Color(240, 240, 240));
        sidePanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        sidePanel.setVisible(false); // Initially hidden
        
        // Add panels to frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(sidePanel, BorderLayout.EAST);

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

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(new Color(0, 120, 215));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 30));
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private JPanel createLabeledField(String labelText, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(label, BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            statusLabel.setText("Connected");
            statusLabel.setForeground(new Color(0, 150, 0));
        } else {
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.RED);
        }
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
        sidePanel.setVisible(false); // Hide user list when disconnected
        frame.pack(); // Update frame size
        sidePanel.setVisible(false);
        typingLabel.setText(" ");
        typingTimer.stop();
        isTyping = false;
        frame.pack();
        
        // Clear the streams
        in = null;
        out = null;
        socket = null;
        incomingReader = null;
        userListModel.clear(); // Clear user list
        updateConnectionStatus(false);
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

                // Send username first
                out.println(username);
                out.flush();

                sendButton.setEnabled(true);
                connectButton.setText("Disconnect");
                messageInput.setEnabled(true);
                usernameField.setEnabled(false);
                serverField.setEnabled(false);
                portField.setEnabled(false);
                isConnected = true;

                out.println("[System] " + username + " has joined the chat");
                out.flush();
                sidePanel.setVisible(true); // Show user list when connected
                frame.pack(); // Update frame size

                incomingReader = new IncomingReader();
                Thread readerThread = new Thread(incomingReader);
                readerThread.start();
                updateConnectionStatus(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to connect to server.");
                ex.printStackTrace();
            }
        } else if (e.getActionCommand().equals("Disconnect")) {
            disconnectFromServer();
            updateConnectionStatus(false);
        }
    }

    private void sendMessage() {
        if (out != null && !messageInput.getText().isEmpty()) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String message = "[" + time + "] " + username + ": " + messageInput.getText();
            out.println(message);
            out.flush();
            messageInput.setText("");
            
            // Send stop typing notification
            if (isTyping) {
                out.println("[STOP_TYPING]");
                out.flush();
                isTyping = false;
            }
            typingTimer.stop();
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
                while (isConnected) {
                    message = in.readLine();
                    if (message == null) break;
                    
                    final String finalMessage = message;
                    if (finalMessage.startsWith("[USERLIST]")) {
                        // Update user list
                        SwingUtilities.invokeLater(() -> {
                            userListModel.clear();
                            String[] users = finalMessage.substring(10).split(",");
                            for (String user : users) {
                                if (!user.isEmpty()) {
                                    userListModel.addElement(user);
                                }
                            }
                        });
                    } else if (finalMessage.endsWith("is typing...")) {
                        // Update typing indicator
                        SwingUtilities.invokeLater(() -> {
                            typingLabel.setText(finalMessage);
                        });
                    } else if (finalMessage.isEmpty()) {
                        // Clear typing indicator
                        SwingUtilities.invokeLater(() -> {
                            typingLabel.setText(" ");
                        });
                    } else {
                        messageArea.append(finalMessage + "\n");
                    }
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
