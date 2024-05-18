import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicaRequestHandler implements RequestHandler {
    @Override
    public void handleRequest(int port) {
        PrintWriter out = null;
        try {
            Socket socket = new Socket("localhost", 6379);
            out = new PrintWriter(socket.getOutputStream(), true);
            // Send a message to the server
            out.println("*1\r\n$4\r\nPING");
            out.flush();
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
