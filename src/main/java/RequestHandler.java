import java.net.Socket;
import java.util.Map;

public interface RequestHandler {
    void handleRequest(Socket socket);
    Map<String, Object> getInfoMap();
}
