package space.kiibou.net.server;

import processing.data.JSONObject;

import java.util.Objects;

public class JSONMessage {

    private final long connectionHandle;
    private final JSONObject message;

    public JSONMessage(long connectionHandle, JSONObject message) {
        Objects.requireNonNull(message);
        this.connectionHandle = connectionHandle;
        this.message = message;
    }

    public JSONObject getMessage() {
        return message;
    }

    public long getConnectionHandle() {
        return connectionHandle;
    }
}
