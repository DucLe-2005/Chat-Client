import java.io.*;
import java.net.*;
import java.util.*;

// Name: Duc Le
// Date: 4/30/2025

/**
 * Chat Server Application
 * This class implements a multi-threaded chat server that can handle multiple clients,
 * broadcast messages, and manage online users.
 */
public class Lab4Server {
    // Server Configuration
    private static final int PORT = 5555;  // Port number for server socket
    
    // Client Management
    private static List<PrintWriter> clientWriters = new ArrayList<>();  // List of client output streams
    private static List<String> onlineUsers = new ArrayList<>();  // List of online usernames

    /**
     * Main method to start the chat server
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Server is running...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Accept new client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);
                
                // Create and start a new thread for the client
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ClientHandler class to manage individual client connections
     * Each instance runs in its own thread and handles communication with one client
     */
    private static class ClientHandler implements Runnable {
        private Socket socket;  // Client socket connection
        private PrintWriter out;  // Output stream to client
        private BufferedReader in;  // Input stream from client
        private String username;  // Client's username

        /**
         * Constructor for ClientHandler
         * @param socket The client socket connection
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Main thread method to handle client communication
         */
        @Override
        public void run() {
            try {
                // Initialize streams
                out = new PrintWriter(socket.getOutputStream(), false);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Get username from client
                username = in.readLine();
                if (username != null && !username.isEmpty()) {
                    // Add client to online users and writers lists
                    synchronized (onlineUsers) {
                        onlineUsers.add(username);
                    }
                    synchronized (clientWriters) {
                        clientWriters.add(out);
                    }
                    broadcastUserList();  // Update all clients with new user list
                }

                // Process messages from client
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("[TYPING]")) {
                        // Broadcast typing notification to all other clients
                        broadcastToOthers(username + " is typing...", out);
                    } else if (message.startsWith("[STOP_TYPING]")) {
                        // Broadcast stop typing notification to all other clients
                        broadcastToOthers("", out);
                    } else if (message.startsWith("[USERLIST]")) {
                        // Ignore user list messages from clients
                        continue;
                    } else {
                        System.out.println("Received: " + message);
                        broadcast(message);  // Broadcast message to all clients
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection lost.");
            } finally {
                // Clean up resources when client disconnects
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Remove client from lists
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
                synchronized (onlineUsers) {
                    onlineUsers.remove(username);
                }
                broadcastUserList();  // Update all clients with new user list
            }
        }
        
        /**
         * Broadcasts a message to all connected clients
         * @param message The message to broadcast
         */
        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                    writer.flush();
                }
            }
        }

        /**
         * Broadcasts a message to all clients except the specified one
         * @param message The message to broadcast
         * @param exclude The writer to exclude from the broadcast
         */
        private void broadcastToOthers(String message, PrintWriter exclude) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    if (writer != exclude) {
                        writer.println(message);
                        writer.flush();
                    }
                }
            }
        }

        /**
         * Broadcasts the current list of online users to all clients
         */
        private void broadcastUserList() {
            StringBuilder userListMessage = new StringBuilder("[USERLIST]");
            synchronized (onlineUsers) {
                for (String user : onlineUsers) {
                    userListMessage.append(user).append(",");
                }
            }
            String finalMessage = userListMessage.toString();
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(finalMessage);
                    writer.flush();
                }
            }
        }
    }
}

