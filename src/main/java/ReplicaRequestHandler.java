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
            while (true) {
                clientSocket.getOutputStream().write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.UTF_8));
                clientSocket.getOutputStream().flush();
//                InputStreamReader inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
//                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                byte[] buffer = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(buffer);
                String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                System.out.println("Response1 " + response);
                clientSocket.getOutputStream().write(("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n" + port + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                byte[] buffer2 = new byte[1024];
                int bytesRead2 = clientSocket.getInputStream().read(buffer2);
                String response2 = new String(buffer2, 0, bytesRead2, StandardCharsets.UTF_8);
                System.out.println("Response1 " + response2);
                clientSocket.getOutputStream().write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes(StandardCharsets.UTF_8));
                clientSocket.getOutputStream().flush();
                byte[] buffer3 = new byte[1024];
                int bytesRead3 = clientSocket.getInputStream().read(buffer3);
                String response3 = new String(buffer3, 0, bytesRead3, StandardCharsets.UTF_8);
                System.out.println("Response1 " + response3);
                if (response.contains("OK")) {
                    clientSocket.getOutputStream().write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes(StandardCharsets.UTF_8));
                    clientSocket.getOutputStream().flush();
                    break;
                }
            }
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
