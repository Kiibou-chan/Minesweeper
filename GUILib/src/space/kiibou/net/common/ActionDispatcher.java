package space.kiibou.net.common;

import processing.data.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class ActionDispatcher<T> {

    private final Map<String, Callbacks<T, Void>> actionCallbackMap;

    public ActionDispatcher() {
        actionCallbackMap = Collections.synchronizedMap(new HashMap<>());
    }

    public abstract void messageReceived(final JSONObject obj);

    public void dispatchAction(final String action, final T jsonMessage) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(jsonMessage);

        if (actionCallbackMap.containsKey(action)) {
            actionCallbackMap.get(action).callAll(jsonMessage);
        }
    }

    public long addActionCallback(final String action, final Consumer<T> callback) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(callback);

        if (!actionCallbackMap.containsKey(action)) {
            actionCallbackMap.put(action, new Callbacks<>());
        }

        return actionCallbackMap.get(action).addCallback(message -> {
            callback.accept(message);
            return null;
        });
    }

    public void removeActionCallback(final String action, final int callbackHandle) {
        Objects.requireNonNull(action);

        if (actionCallbackMap.containsKey(action)) {
            actionCallbackMap.get(action).removeCallback(callbackHandle);
        }
    }

}
