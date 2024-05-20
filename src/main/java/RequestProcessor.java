import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestProcessor {
    private final Map<String, Object> storage;
    private final int port;
    private final Map<String, Object> infoMap;
    private final String REPL_ID = "REPL";
    private final int OFFSET = 0;
    private final Role role;
    private final Map<String, Socket> replicaMap;
    public RequestProcessor(Map<String, Object> storage, int port, Map<String, Object> infoMap, Role role) {
        this.storage = storage;
        this.port = port;
        this.infoMap = infoMap;
        this.role = role;
        replicaMap = new ConcurrentHashMap<>();
    }

    public void handleRequest() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> {
                    handleRequest(clientSocket);
                });
            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }

    }

    public void handleRequest(Socket clientSocket) {
        try {
            while (clientSocket.isConnected()) {
                byte[] input = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(input);
                String rawRequest = new String(input, 0, bytesRead);
                String request = rawRequest.trim();
                String[] parts = request.split("\r\n");
                if (parts.length >= 2) {
                    if (parts[2].equalsIgnoreCase("SET")) {
                        String key = parts[4];
                        String value = parts[6];
                        long timeout = parts.length == 11 ? Long.parseLong(parts[10]) : 0;
                        Data data = new Data(value, timeout);
                        storage.put(key, data);
                        String response = "OK";
                        clientSocket.getOutputStream().write(("$" + response.length() + "\r\n" + response + "\r\n")
                                .getBytes());
                        if (Role.MASTER.name().equals(role.name())) {
                            sendToReplicas(rawRequest);
                        }
                    } else if (parts[2].equalsIgnoreCase("GET")) {
                        Object rawData = storage.get(parts[4]);
                        String value = null;
                        if (Objects.nonNull(rawData)) {
                            Data data = (Data) rawData;
                            if (data.expiry > 0 && data.expiry < System.currentTimeMillis()) {
                                storage.remove(parts[4]);
                            } else {
                                value = (String) data.value;
                            }
                        }
                        if (Objects.nonNull(value)) {
                            clientSocket.getOutputStream().write(
                                    ("$" + value.length() + "\r\n" + value + "\r\n").getBytes());
                        } else {
                            clientSocket.getOutputStream().write(
                                    ("$-1\r\n").getBytes());
                        }
                    } else if (parts[2].equalsIgnoreCase("INFO")) {
                        StringBuilder builder = new StringBuilder();
                        infoMap.forEach((key, value) -> builder.append(key).append(":").append(value));
                        String value = builder.toString();
                        clientSocket.getOutputStream().write(
                                ("$" + value.length() + "\r\n" + value + "\r\n").getBytes());
                    } else if (parts[2].equalsIgnoreCase("REPLCONF") && Role.MASTER.name().equals(role.name())) {
                        String data = "OK";
                        clientSocket.getOutputStream().write(
                                ("$" + data.length() + "\r\n" + data + "\r\n").getBytes());
                    } else if (parts[2].equalsIgnoreCase("PSYNC") && Role.MASTER.name().equals(role.name())) {
                        String replID = REPL_ID + UUID.randomUUID().toString().substring(25);
                        String data = String.format("+FULLRESYNC %s %d%s", replID, OFFSET, "\r\n");
                        clientSocket.getOutputStream().write(data.getBytes());
                        clientSocket.getOutputStream().flush();
                        String fileContents = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
                        byte[] bytes = Base64.getDecoder().decode(fileContents);
                        clientSocket.getOutputStream().write(("$" + bytes.length + "\r\n").getBytes());
                        clientSocket.getOutputStream().flush();
                        clientSocket.getOutputStream().write(bytes);
                        clientSocket.getOutputStream().flush();
                        replicaMap.put(replID, clientSocket);
                    } else if (parts[2].equalsIgnoreCase("ECHO")) {
                        String data = parts[4];
                        clientSocket.getOutputStream().write(
                                ("$" + data.length() + "\r\n" + data + "\r\n").getBytes());
                    } else if (parts[2].equalsIgnoreCase("PING")) {
                        System.out.println("Sending Pong....");
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
        }
    }

    private void sendToReplicas(String request) {
        replicaMap.forEach((replicaId, socket) -> {
            CompletableFuture.runAsync(() -> {
                try {
                    socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    String response = ReplicaRequestHandler.getResponse(socket);
                    System.out.println(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
