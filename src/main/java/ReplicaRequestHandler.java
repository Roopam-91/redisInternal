import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicaRequestHandler implements RequestHandler {

    private final Map<String, Object> storage;

    public ReplicaRequestHandler() {
        storage = new ConcurrentHashMap<>();
    }
    @Override
    public void handleRequest(int port) {
        sendPingToMaster();
        RequestProcessor requestProcessor = new RequestProcessor(storage, port, getInfoMap());
        requestProcessor.handleRequest();
    }

    private void sendPingToMaster() {
        Socket clientSocket;
        try {
            clientSocket = new Socket("localhost", 6379);
            clientSocket.setReuseAddress(true);
            // Send a message to the server
            clientSocket.getOutputStream().write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.UTF_8));
            clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Map<String, Object> getInfoMap() {
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("role", "slave");
        return map;
    }
}
