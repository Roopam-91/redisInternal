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
        sendMessageToMaster(port);
        RequestProcessor requestProcessor = new RequestProcessor(storage, port, getInfoMap(), Role.REPLICA);
        requestProcessor.handleRequest();
    }

    private void sendMessageToMaster(int port) {
        Socket clientSocket;
        try {
            clientSocket = new Socket("localhost", 6379);
            clientSocket.setReuseAddress(true);
            // Send a message to the server
            clientSocket.getOutputStream().write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.UTF_8));
            clientSocket.getOutputStream().flush();
            String response = getResponse(clientSocket);
            System.out.println(response);
            clientSocket.getOutputStream().write(("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n" + port + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            String response2 = getResponse(clientSocket);
            System.out.println(response2);
            clientSocket.getOutputStream().write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes(StandardCharsets.UTF_8));
            clientSocket.getOutputStream().flush();
            String response3 = getResponse(clientSocket);
            System.out.println(response3);
            if (response3.contains("OK")) {
                clientSocket.getOutputStream().write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes(StandardCharsets.UTF_8));
                clientSocket.getOutputStream().flush();
                String response4 = getResponse(clientSocket);
                System.out.println(response4);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getResponse(Socket clientSocket) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = clientSocket.getInputStream().read(buffer);
        return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
    }


    @Override
    public Map<String, Object> getInfoMap() {
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("role", "slave");
        return map;
    }
}
