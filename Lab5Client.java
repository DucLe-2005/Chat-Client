import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Name: Duc Le
// Date: 4/30/2025
/**
 * Chat Client Application
 * This class implements a GUI-based chat client that can connect to a chat server,
 * send and receive messages, and display online users.
 */
public class Lab5Client {
    // GUI Components
    private JFrame frame = new JFrame("Chat Client");
    private JTextArea messageArea = new JTextArea(20, 50);  // Main chat display area
    private JTextField messageInput = new JTextField(50);    // Message input field
    private JTextField serverField = new JTextField("localhost", 10);  // Server address input
    private JTextField portField = new JTextField("5555", 5);  // Port number input
    private JTextField usernameField = new JTextField(15);   // Username input
    private JButton sendButton = new JButton("Send");        // Send message button
    private JButton connectButton = new JButton("Connect");  // Connect/Disconnect button
    private JLabel statusLabel = new JLabel("Disconnected"); // Connection status indicator
    private DefaultListModel<String> userListModel = new DefaultListModel<>();  // Model for online users list
    private JList<String> userList = new JList<>(userListModel);  // Display list of online users
    private JPanel sidePanel;  // Panel containing user list
    private JLabel typingLabel = new JLabel(" ");  // Label to show typing status
    private Timer typingTimer;  // Timer for typing status timeout
    private static final int TYPING_TIMEOUT = 3000;  // 3 seconds timeout for typing indicator
    private boolean isTyping = false;  // Flag to track typing status

    // Network Components
    private Socket socket;  // Socket connection to server
    private BufferedReader in;  // Input stream from server
    private PrintWriter out;  // Output stream to server
    private String username;  // Current user's username
    private boolean isConnected = false;  // Connection status flag
    private IncomingReader incomingReader;  // Thread for reading incoming messages

    /**
     * Constructor - Initializes the GUI and sets up event listeners
     */
    public Lab5Client() {
        initializeGUI();
        setupEventListeners();
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /**
     * Initializes the GUI components and layout
     */
    private void initializeGUI() {
        // Set up the main frame
        frame.setLayout(new BorderLayout());
        frame.setBackground(new Color(240, 240, 240));
        
        // Configure message area
        configureMessageArea();
        
        // Configure input field
        configureInputField();
        
        // Configure buttons and text fields
        styleComponents();
        
        // Create and configure panels
        createPanels();
    }

    /**
     * Sets up event listeners for buttons and input fields
     */
    private void setupEventListeners() {
        // Connect button action listener
        connectButton.addActionListener(e -> handleConnect(e));
        
        // Send button action listener
        sendButton.addActionListener(e -> sendMessage());
        
        // Message input typing listener
        setupTypingListener();
        
        // Window closing listener
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectFromServer();
            }
        });
    }

    /**
     * Handles connection/disconnection to/from the server
     * @param e ActionEvent from the connect button
     */
    private void handleConnect(ActionEvent e) {
        if (e.getActionCommand().equals("Connect")) {
            connectToServer();
        } else if (e.getActionCommand().equals("Disconnect")) {
            disconnectFromServer();
            updateConnectionStatus(false);
        }
    }

    /**
     * Establishes connection to the server
     */
    private void connectToServer() {
        if (usernameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a username first.");
            return;
        }
        
        try {
            // Initialize connection
            initializeConnection();
            
            // Send username to server
            sendUsername();
            
            // Update UI for connected state
            updateUIForConnectedState();
            
            // Start message reader thread
            startMessageReader();
            
            updateConnectionStatus(true);
        } catch (IOException ex) {
            handleConnectionError(ex);
        }
    }

    /**
     * Disconnects from the server and cleans up resources
     */
    private void disconnectFromServer() {
        if (isConnected && out != null) {
            try {
                // Send disconnect message
                sendDisconnectMessage();
                
                // Stop message reader
                stopMessageReader();
                
                // Close socket
                closeSocket();
                
                // Clean up UI and resources
                cleanupConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a message to the server
     */
    private void sendMessage() {
        if (out != null && !messageInput.getText().isEmpty()) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String message = "[" + time + "] " + username + ": " + messageInput.getText();
            out.println(message);
            out.flush();
            messageInput.setText("");
            
            // Handle typing status
            handleTypingStatusAfterMessage();
        }
    }

    /**
     * Thread for reading incoming messages from the server
     */
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
                    handleIncomingMessage(message);
                } 
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanupConnection();
            }
        }
    }

    /**
     * Main method to start the chat client
     */
    public static void main(String[] args) {
        Lab5Client client = new Lab5Client();
    }

    /**
     * Configures the message area with appropriate styling
     */
    private void configureMessageArea() {
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(new Color(255, 255, 255));
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Configures the input field with appropriate styling
     */
    private void configureInputField() {
        messageInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    /**
     * Styles all components with consistent look and feel
     */
    private void styleComponents() {
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
    }

    /**
     * Creates and configures all panels in the GUI
     */
    private void createPanels() {
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
        bottomPanel.add(typingLabel, BorderLayout.SOUTH);
        
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
        sidePanel.setVisible(false);
        
        // Add panels to frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(sidePanel, BorderLayout.EAST);
    }

    /**
     * Sets up the typing listener for the message input
     */
    private void setupTypingListener() {
        typingTimer = new Timer(TYPING_TIMEOUT, e -> {
            if (isTyping && isConnected && out != null) {
                out.println("[STOP_TYPING]");
                out.flush();
                isTyping = false;
            }
        });
        typingTimer.setRepeats(false);
        
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
    }

    /**
     * Updates the connection status label
     * @param connected true if connected, false if disconnected
     */
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            statusLabel.setText("Connected");
            statusLabel.setForeground(new Color(0, 150, 0));
        } else {
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.RED);
        }
    }

    /**
     * Initializes the connection to the server
     */
    private void initializeConnection() throws IOException {
        username = usernameField.getText().trim();
        socket = new Socket(serverField.getText(), Integer.parseInt(portField.getText()));
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), false);
    }

    /**
     * Sends the username to the server
     */
    private void sendUsername() {
        out.println(username);
        out.flush();
    }

    /**
     * Updates the UI for connected state
     */
    private void updateUIForConnectedState() {
        sendButton.setEnabled(true);
        connectButton.setText("Disconnect");
        messageInput.setEnabled(true);
        usernameField.setEnabled(false);
        serverField.setEnabled(false);
        portField.setEnabled(false);
        isConnected = true;
        sidePanel.setVisible(true);
        frame.pack();
    }

    /**
     * Starts the message reader thread
     */
    private void startMessageReader() {
        incomingReader = new IncomingReader();
        Thread readerThread = new Thread(incomingReader);
        readerThread.start();
    }

    /**
     * Handles incoming messages from the server
     * @param message The message received from the server
     */
    private void handleIncomingMessage(String message) {
        if (message.startsWith("[USERLIST]")) {
            // Update user list
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                String[] users = message.substring(10).split(",");
                for (String user : users) {
                    if (!user.isEmpty()) {
                        userListModel.addElement(user);
                    }
                }
            });
        } else if (message.endsWith("is typing...")) {
            // Update typing indicator
            SwingUtilities.invokeLater(() -> {
                typingLabel.setText(message);
            });
        } else if (message.isEmpty()) {
            // Clear typing indicator
            SwingUtilities.invokeLater(() -> {
                typingLabel.setText(" ");
            });
        } else {
            messageArea.append(message + "\n");
        }
    }

    /**
     * Handles typing status after sending a message
     */
    private void handleTypingStatusAfterMessage() {
        if (isTyping) {
            out.println("[STOP_TYPING]");
            out.flush();
            isTyping = false;
        }
        typingTimer.stop();
    }

    /**
     * Sends disconnect message to the server
     */
    private void sendDisconnectMessage() {
        out.println("[System] " + username + " has left the chat");
        out.flush();
    }

    /**
     * Stops the message reader thread
     */
    private void stopMessageReader() {
        if (incomingReader != null) {
            incomingReader.stop();
        }
    }

    /**
     * Closes the socket connection
     */
    private void closeSocket() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Cleans up connection resources and resets UI state
     */
    private void cleanupConnection() {
        isConnected = false;
        sendButton.setEnabled(false);
        messageInput.setEnabled(false);
        connectButton.setEnabled(true);
        usernameField.setEnabled(true);
        connectButton.setText("Connect");
        sidePanel.setVisible(false);
        frame.pack();
        typingLabel.setText(" ");
        typingTimer.stop();
        isTyping = false;
        
        // Clear the streams
        in = null;
        out = null;
        socket = null;
        incomingReader = null;
        userListModel.clear();
        updateConnectionStatus(false);
    }

    /**
     * Handles connection errors by showing an error message
     * @param ex The IOException that occurred
     */
    private void handleConnectionError(IOException ex) {
        JOptionPane.showMessageDialog(frame, "Failed to connect to server.");
        ex.printStackTrace();
    }

    /**
     * Styles a button with consistent look and feel
     * @param button The button to style
     */
    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(new Color(0, 120, 215));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 30));
    }

    /**
     * Styles a text field with consistent look and feel
     * @param field The text field to style
     */
    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    /**
     * Creates a labeled field panel
     * @param labelText The text for the label
     * @param field The text field to add
     * @return A panel containing the label and field
     */
    private JPanel createLabeledField(String labelText, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(label, BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
}
