import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterRequestHandler implements RequestHandler {
    private final Map<String, Object> storage;

    public MasterRequestHandler() {
        storage = new ConcurrentHashMap<>();
    }

    @Override
    public void handleRequest(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            while (true) {
                executor.submit(() -> {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleRequest(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private void handleRequest(Socket clientSocket) {
        try {
            while (clientSocket.isConnected()) {
                byte[] input = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(input);
                String request = new String(input, 0, bytesRead).trim();
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
                    }
                    else if (parts[2].equalsIgnoreCase("GET")) {
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
                    }
                    else if (parts[2].equalsIgnoreCase("INFO")) {
                        Map<String, Object> infoMap = getInfoMap();
                        StringBuilder builder = new StringBuilder();
                        infoMap.forEach((key, value) -> builder.append(key).append(":").append(value));
                        String value = builder.toString();
                        clientSocket.getOutputStream().write(
                                ("$" + value.length() + "\r\n" + value + "\r\n").getBytes());
                    }
                    else if (parts[2].equalsIgnoreCase("ECHO")) {
                        String data = parts[4];
                        clientSocket.getOutputStream().write(
                                ("$" + data.length() + "\r\n" + data + "\r\n").getBytes());
                    }
                    else if (parts[2].equalsIgnoreCase("PING")) {
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

    @Override
    public Map<String, Object> getInfoMap() {
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("role", "master");
        map.put("master_replid", "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb");
        map.put("master_repl_offset", 0);
        return map;
    }
}
