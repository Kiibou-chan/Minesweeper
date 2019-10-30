package space.kiibou.net.client;

import processing.data.JSONObject;
import space.kiibou.net.common.SocketConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

public class Client {
    private final Runnable onConnect;
    private final Consumer<JSONObject> onMessageReceived;
    private final Runnable onDisconnect;

    private Socket socket;
    private SocketConnection connection;

    public Client(final Runnable onConnect, final Consumer<JSONObject> onMessageReceived, final Runnable onDisconnect) {
        this.onConnect = Objects.requireNonNull(onConnect);
        this.onMessageReceived = Objects.requireNonNull(onMessageReceived);
        this.onDisconnect = Objects.requireNonNull(onDisconnect);
    }

    public Client connect(final String address, final int port) {
        try {
            socket = new Socket(Objects.requireNonNull(address), port);

            SocketConnection.create(socket).ifPresent((final SocketConnection conn) -> {
                onConnect.run();
                connection = conn;
                conn.registerMessageCallback((handle, message) -> {
                    JSONObject jsonObject = JSONObject.parse(message);
                    onMessageReceived.accept(jsonObject);
                });
                conn.registerDisconnectCallback(handle -> onDisconnect.run());
            });
        } catch (IOException ex) {
            System.out.printf("Failed to connect to %s:%d%n", address, port);
        }

        return this;
    }

    public void stop() throws IOException {
        socket.close();
    }

    public void sendMessage(final String message) {
        Objects.requireNonNull(message);
        if (connection != null) {
            connection.sendMessage(message);
        }
    }

    public void sendJSON(final JSONObject object) {
        Objects.requireNonNull(object);
        String message = object.format(-1);
        sendMessage(message);
    }

}
