package space.kiibou.net.common;

public class TextMessage {

    private final long connectionHandle;
    private final String message;

    public TextMessage(long connectionHandle, String message) {
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
