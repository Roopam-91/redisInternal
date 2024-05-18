public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        int port = args.length >= 2 && args[0].equals("--port") ? Integer.parseInt(args[1]) : 6379;
        boolean isReplica = args.length >=3 && args[2].equals("--replicaof");
        RequestHandler requestHandler = isReplica ? new ReplicaRequestHandler() : new MasterRequestHandler();
        requestHandler.handleRequest(port);
    }

}