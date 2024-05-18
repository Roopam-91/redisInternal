public class Data {
    Object value;
    long expiry;

    public Data(Object value) {
        this(value, 0);
    }

    public Data(Object value, long timeout) {
        this.value = value;
        long insertTs = System.currentTimeMillis();
        if (timeout != 0) {
            this.expiry = insertTs + timeout;
        }
    }
}
