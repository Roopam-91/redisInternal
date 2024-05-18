import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicaRequestHandler implements RequestHandler {

    private final Map<String, Object> storage;

    public ReplicaRequestHandler() {
        storage = new ConcurrentHashMap<>();
    }
    @Override
    public void handleRequest(int port) {
        sendMessageToMaster(port);
        RequestProcessor requestProcessor = new RequestProcessor(storage, port, getInfoMap());
        requestProcessor.handleRequest();
    }

    private void sendMessageToMaster(int port) {
        Socket clientSocket;
        try {
            clientSocket = new Socket("localhost", 6379);
            clientSocket.setReuseAddress(true);
            // Send a message to the server
            clientSocket.getOutputStream().write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.UTF_8));
            clientSocket.getOutputStream().write(("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n"+port+"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            clientSocket.getOutputStream().write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes(StandardCharsets.UTF_8));
            String response = new String(clientSocket.getInputStream().readAllBytes());
            if (response.contains("OK")) {
                clientSocket.getOutputStream().write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes(StandardCharsets.UTF_8));
            }
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
