public class Replica {
    private String hostName;
    private String replId;
    private int port;
    public Replica(String hostName, String replId, int port) {
        this.hostName = hostName;
        this.replId = replId;
        this.port = port;

    }
    public Replica() {}

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getReplId() {
        return replId;
    }

    public void setReplId(String replId) {
        this.replId = replId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
