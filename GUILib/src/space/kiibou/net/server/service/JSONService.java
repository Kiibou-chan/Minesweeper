package space.kiibou.net.server.service;

import processing.data.JSONObject;
import space.kiibou.net.common.Callbacks;
import space.kiibou.net.server.Server;
import space.kiibou.net.server.Service;

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

        jsonMessageCallbacks.callAll(result);
    }

    public long addJSONMessageReceivedCallback(final Consumer<JSONObject> callback) {
        Objects.requireNonNull(callback);

        return jsonMessageCallbacks.addCallback(message -> {
            callback.accept(message);
            return null;
        });
    }

    public void removeJSONMessageReceivedCallback(final long callbackHandle) {
        jsonMessageCallbacks.removeCallback(callbackHandle);
    }

    public void sendJSONObject(final long handle, final JSONObject object) {
        Objects.requireNonNull(object);
        server.sendMessage(handle, object.format(-1));
    }

    public void broadcastJSONObject(final JSONObject object) {
        Objects.requireNonNull(object);
        server.broadcastMessage(object.format(-1));
    }

}
