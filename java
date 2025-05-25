import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * ChatApp - A simple multi-client chat server and client implementation.
 */
public class ChatApp {

    // ===================== SERVER =====================
    public static class Server {
        private ServerSocket serverSocket;
        private static final ArrayList<ClientHandler> clients = new ArrayList<>();

        public Server(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        /**
         * Starts accepting client connections.
         */
        public void startServer() {
            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    System.out.println("A new client has connected!");

                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);

                    Thread thread = new Thread(clientHandler);
                    thread.start();
                }
            } catch (IOException e) {
                closeServerSocket();
            }
        }

        /**
         * Closes the server socket safely.
         */
        public void closeServerSocket() {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Broadcasts a message to all connected clients.
         */
        public static void broadcastMessage(String message) {
            for (ClientHandler client : clients) {
                if (client != null) {
                    client.sendMessage(message);
                }
            }
        }

        /**
         * Removes a client handler from the list of active clients.
         */
        public static void removeClientHandler(ClientHandler clientHandler) {
            clients.remove(clientHandler);
            System.out.println("A client has disconnected.");
        }

        /**
         * Server entry point.
         */
        public static void start() throws IOException {
            ServerSocket serverSocket = new ServerSocket(1234);
            Server server = new Server(serverSocket);
            System.out.println("Server is running on port 1234...");
            server.startServer();
        }
    }

    /**
     * Handles individual client connections.
     */
    public static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String clientUsername;

        public ClientHandler(Socket socket) {
            try {
                this.socket = socket;
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                this.clientUsername = bufferedReader.readLine();  // First line is username
                if (clientUsername == null) {
                    closeEverything();
                    return;
                }

                Server.broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
            } catch (IOException e) {
                closeEverything();
            }
        }

        @Override
        public void run() {
            String messageFromClient;

            while (socket.isConnected()) {
                try {
                    messageFromClient = bufferedReader.readLine();
                    if (messageFromClient == null) break;

                    Server.broadcastMessage(messageFromClient);
                } catch (IOException e) {
                    break;
                }
            }

            closeEverything();
        }

        /**
         * Sends a message to the client.
         */
        public void sendMessage(String message) {
            try {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything();
            }
        }

        /**
         * Closes resources and removes the client.
         */
        public void closeEverything() {
            Server.removeClientHandler(this);
            try {
                if (bufferedReader != null) bufferedReader.close();
                if (bufferedWriter != null) bufferedWriter.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ===================== CLIENT =====================
    public static class Client {
        private Socket socket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String username;

        public Client(Socket socket, String username) {
            try {
                this.socket = socket;
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.username = username;
            } catch (IOException e) {
                closeEverything();
            }
        }

        /**
         * Sends messages from user input to the server.
         */
        public void sendMessage() {
            try {
                bufferedWriter.write(username);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                Scanner scanner = new Scanner(System.in);

                while (socket.isConnected()) {
                    String messageToSend = scanner.nextLine();
                    bufferedWriter.write(username + ": " + messageToSend);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything();
            }
        }

        /**
         * Starts a thread to continuously listen for incoming messages from the server.
         */
        public void listenForMessage() {
            new Thread(() -> {
                String messageFromChat;

                while (socket.isConnected()) {
                    try {
                        messageFromChat = bufferedReader.readLine();
                        System.out.println(messageFromChat);
                    } catch (IOException e) {
                        closeEverything();
                        break;
                    }
                }
            }).start();
        }

        /**
         * Closes client resources.
         */
        public void closeEverything() {
            try {
                if (bufferedReader != null) bufferedReader.close();
                if (bufferedWriter != null) bufferedWriter.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Client entry point.
         */
        public static void start() throws IOException {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();

            Socket socket = new Socket("localhost", 1234);
            Client client = new Client(socket, username);

            client.listenForMessage();
            client.sendMessage();
        }
    }

    // ===================== ENTRY POINT =====================
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Start as (server/client): ");
        String mode = scanner.nextLine().trim().toLowerCase();

        if (mode.equals("server")) {
            Server.start();
        } else if (mode.equals("client")) {
            Client.start();
        } else {
            System.out.println("Invalid option. Please enter 'server' or 'client'.");
        }
    }
}
