package space.kiibou.net.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.Objects;
import java.util.function.Consumer;

public class InputStreamListener implements Runnable {

    private final InputStream in;
    private final Callbacks<String, Void> messageCallbacks;
    private final Callbacks<Void, Void> disconnectCallbacks;

    public InputStreamListener(final InputStream in) {
        this.in = Objects.requireNonNull(in);
        messageCallbacks = new Callbacks<>();
        disconnectCallbacks = new Callbacks<>();
    }

    @Override
    public void run() {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            while (!Thread.interrupted()) {
                final String message = reader.readLine();

                Objects.requireNonNull(message);

                messageCallbacks.callAll(message);
            }
        } catch (SocketException | NullPointerException e) {
            disconnectCallbacks.callAll(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long registerMessageCallback(final Consumer<String> callback) {
        return messageCallbacks.addCallback(message -> {
            callback.accept(message);
            return null;
        });
    }

    public long registerDisconnectCallback(final Runnable callback) {
        return disconnectCallbacks.addCallback(ignore -> {
            callback.run();
            return null;
        });
    }

    public void stop() throws IOException {
        Thread.currentThread().interrupt();
        in.close();
    }
}
