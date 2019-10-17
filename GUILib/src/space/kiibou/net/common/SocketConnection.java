package space.kiibou.net.common;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SocketConnection {
    private final PrintWriter out;
    private final InputStreamListener listener;

    private final long handle;

    private static long cHandle = 0;

    private static long nextHandle() {
        return cHandle++;
    }

    public static Optional<SocketConnection> create(final Socket socket) {
        try {
            return Optional.of(new SocketConnection(Objects.requireNonNull(socket)));
        } catch (IOException e) {
            System.err.println("Failed to create Connection!");
            return Optional.empty();
        }
    }

    private SocketConnection(final Socket socket) throws IOException {
        Objects.requireNonNull(socket);

        handle = nextHandle();

        listener = new InputStreamListener(socket.getInputStream());
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

        Thread listenerThread = new Thread(listener, "Listener-Thread " + handle);
        listenerThread.start();
    }

    public void registerMessageCallback(final BiConsumer<Long, String> callback) {
        Objects.requireNonNull(callback);
        listener.registerMessageCallback((message) -> callback.accept(handle, message));
    }

    public void registerDisconnectCallback(final Consumer<Long> callback) {
        Objects.requireNonNull(callback);
        listener.registerDisconnectCallback(() -> callback.accept(handle));
    }

    public void sendMessage(final String message) {
        out.println(Objects.requireNonNull(message));
        out.flush();
    }

    public long getHandle() {
        return handle;
    }
}
