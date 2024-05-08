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
        InputStream input = clientSocket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(input);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        OutputStream output = clientSocket.getOutputStream();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            String[] parts = line.split("\r\n");
            String command = parts[2];
            if (command.equalsIgnoreCase("ECHO")) {
                String data = parts[4];
                int length = data.length();
                String result = "$"+length +"\\r\\n" + data + "\\r\\n";
                System.out.println(result);
                output.write(
                        ("$" + data.length() + "\r\n" + data + "\r\n").getBytes());
            } else {
                clientSocket.getOutputStream().write(
                        "-ERR unknown command\r\n".getBytes());
            }
        }
    }
}
