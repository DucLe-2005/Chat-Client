import java.io.*;
import java.net.*;
import java.util.*;

public class Lab4Server {
    private static final int PORT = 5555;
    private static List<PrintWriter> clientWriters = new ArrayList<>();
    private static List<String> onlineUsers = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Server is running...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), false);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // First message should be the username
                username = in.readLine();
                if (username != null && !username.isEmpty()) {
                    synchronized (onlineUsers) {
                        onlineUsers.add(username);
                    }
                    synchronized (clientWriters) {
                        clientWriters.add(out);
                    }
                    broadcastUserList();
                }

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    broadcast(message);
                }
            } catch (IOException e) {
                System.out.println("Connection lost.");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
                synchronized (onlineUsers) {
                    onlineUsers.remove(username);
                }
                broadcastUserList();
            }
        }
        
        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                    writer.flush();
                }
            }
        }

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

