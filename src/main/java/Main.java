import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        //  Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        int port = 6379;
        try {
            // Wait for connection from client.
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> {
                    try {
                        handleRequest(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }


        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket clientSocket) throws IOException {
        try {
            while (clientSocket.isConnected()) {
                byte[] input = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(input);
                String request = new String(input, 0, bytesRead).trim();
                String[] parts = request.split("\r\n");
                if (parts.length >= 2) {
                    if (parts[2].equalsIgnoreCase("ECHO")) {
                        String data = parts[4];
                        clientSocket.getOutputStream().write(
                                ("$" + data.length() + "\r\n" + data + "\r\n").getBytes());
                    } else if (parts[2].equalsIgnoreCase("PING")) {
                        clientSocket.getOutputStream().write("+PONG\r\n".getBytes());
                    } else {
                        clientSocket.getOutputStream().write(
                                "-ERR invalid request\r\n".getBytes());
                    }
                } else {
                    clientSocket.getOutputStream().write(
                            "-ERR invalid request\r\n".getBytes());
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
