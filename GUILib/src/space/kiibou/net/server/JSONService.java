package space.kiibou.net.server;

import processing.data.JSONObject;
import space.kiibou.net.common.Callbacks;

import java.util.Objects;
import java.util.function.Consumer;

public class JSONService extends Service {

    private final Callbacks<JSONObject, Void> jsonMessageCallbacks;

    public JSONService(Server server) {
        super(server);
        server.registerMessageReceivedCallback(this::onMessageReceived);
        jsonMessageCallbacks = new Callbacks<>();
    }

    @Override
    public void initialize() {

    }

    private void onMessageReceived(long handle, String message) {
        JSONObject parsedMessage = JSONObject.parse(message);

        JSONObject result = new JSONObject();

        result.setLong("handle", handle);
        result.setJSONObject("message", parsedMessage);
    }

    public long registerJSONMessageReceivedCallback(final Consumer<JSONObject> callback) {
        Objects.requireNonNull(callback);
        return jsonMessageCallbacks.addCallback(message -> {
            callback.accept(message);
            return null;
        });
    }

    public void broadcastJSONObject(final JSONObject object) {
        server.broadcastMessage(object.format(-1));
    }

}
