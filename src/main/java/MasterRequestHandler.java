import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MasterRequestHandler implements RequestHandler {
    private final Map<String, Object> storage;

    public MasterRequestHandler() {
        storage = new ConcurrentHashMap<>();
    }

    @Override
    public void handleRequest(int port) {
        RequestProcessor requestProcessor = new RequestProcessor(storage, port, getInfoMap());
        requestProcessor.handleRequest();
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
