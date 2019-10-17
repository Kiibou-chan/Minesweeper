package space.kiibou.net.common;

public class Message {

    private final long connectionHandle;
    private final String message;

    public Message(long connectionHandle, String message) {
        this.connectionHandle = connectionHandle;
        this.message = message;
    }

    public long getConnectionHandle() {
        return connectionHandle;
    }

    public String getMessage() {
        return message;
    }

}
