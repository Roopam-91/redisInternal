import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler {
    private final Map<String, Object> storage;
    private boolean isReplica;
    public RequestHandler(boolean isReplica) {
        this.storage = new ConcurrentHashMap<>();
        this.isReplica = isReplica;
    }

    public void handleRequest(Socket clientSocket) throws IOException {
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
                        Map<String, String> infoMap = getInfoMap();
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

    private Map<String, String> getInfoMap() {
        Map<String, String> map = new ConcurrentHashMap<>();
        String role = isReplica ? "slave" : "master";
        map.put("role", role);
        return map;
    }

    static class Data {
        Object value;
        long expiry;
        public Data(Object value) {
            this(value, 0);
        }
        public Data(Object value, long timeout) {
            this.value = value;
            long insertTs = System.currentTimeMillis();
            if (timeout != 0) {
                this.expiry = insertTs + timeout;
            }
        }
    }

}
