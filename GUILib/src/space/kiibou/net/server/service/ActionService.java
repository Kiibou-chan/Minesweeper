package space.kiibou.net.server.service;

import processing.data.JSONObject;
import space.kiibou.net.common.ActionDispatcher;
import space.kiibou.net.server.JSONMessage;
import space.kiibou.net.server.Server;
import space.kiibou.net.server.Service;
import space.kiibou.reflect.Inject;

import java.util.Objects;
import java.util.function.Consumer;

public class ActionService extends Service {

    private final ActionDispatcher<JSONMessage> dispatcher;
    @Inject
    public JSONService json;

    public ActionService(final Server server) {
        super(server);

        dispatcher = new ActionDispatcher<JSONMessage>() {
            @Override
            public void messageReceived(JSONObject obj) {
                Objects.requireNonNull(obj);

                if (obj.hasKey("handle") && obj.hasKey("message")) {
                    final int handle = obj.getInt("handle");
                    final JSONObject message = obj.getJSONObject("message");
                    if (message.hasKey("action")) {
                        final String action = message.getString("action");

                        final JSONMessage jsonMessage = new JSONMessage(handle, message);
                        dispatchAction(action, jsonMessage);
                    }
                }
            }
        };
    }

    @Override
    public void initialize() {
        json.addJSONMessageReceivedCallback(dispatcher::messageReceived);
    }

    public void sendActionToClient(final long handle, final String action) {
        sendActionToClient(handle, action, new JSONObject());
    }

    public void sendActionToClient(final long handle, final String action, final JSONObject message) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(message);

        message.setString("action", action);
        json.sendJSONObject(handle, message);
    }

    public long addActionCallback(final String action, final Consumer<JSONMessage> callback) {
        return dispatcher.addActionCallback(action, callback);
    }

    public void removeActionCallback(final String action, final int callbackHandle) {
        dispatcher.removeActionCallback(action, callbackHandle);
    }

}
