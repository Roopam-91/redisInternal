import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        ServerSocket serverSocket = null;
        int port = args.length >= 2 && args[0].equals("--port") ? Integer.parseInt(args[1]) : 6379;
        boolean isReplica = args.length >=3 && args[2].equals("--replicaof");
        try {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            RequestHandler requestHandler = isReplica ? new ReplicaRequestHandler() : new MasterRequestHandler();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> {
                    requestHandler.handleRequest(clientSocket);
                });
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

}