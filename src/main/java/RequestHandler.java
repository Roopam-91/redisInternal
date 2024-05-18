import java.util.Map;

public interface RequestHandler {
    void handleRequest(int port);
    Map<String, Object> getInfoMap();
}
