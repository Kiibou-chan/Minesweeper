package space.kiibou.net.server;

import space.kiibou.net.common.Callbacks;
import space.kiibou.net.common.SocketConnection;
import space.kiibou.net.common.TextMessage;
import space.kiibou.net.server.service.ActionService;
import space.kiibou.net.server.service.JSONService;
import space.kiibou.reflect.Inject;
import space.kiibou.reflect.ReflectUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class Server {

    private final Map<Long, SocketConnection> connections;
    private final List<Service> services;
    private final Callbacks<TextMessage, Void> messageCallbacks;
    private ServerSocket serverSocket;
    private Thread connectionListenerThread;

    private final Map<String, Service> servicesMap;

    private Server(String... serviceNames) {
        connections = Collections.synchronizedMap(new HashMap<>());
        services = Collections.synchronizedList(new ArrayList<>());
        servicesMap = Collections.synchronizedMap(new HashMap<>());
        messageCallbacks = new Callbacks<>();

        connectionListenerThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Socket conn = serverSocket.accept();
                    register(conn);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "Server-Connection-Listener-Thread");

        for (String serviceName : serviceNames) {
            registerService(serviceName);
        }

        registerService(JSONService.class.getCanonicalName());
        registerService(ActionService.class.getCanonicalName());

        for (Service service : services) injectServices(service);
        for (Service service : services) service.initialize();
    }

    private void injectServices(final Service service) {
        Objects.requireNonNull(service);

        final Field[] fields = ReflectUtils.getAnnotatedFields(service, Inject.class);

        for (Field field : fields) {
            final String name = field.getType().getCanonicalName();
            final Service toInject = servicesMap.get(name);
            System.out.printf("Injecting %s into %s%n", name, service.getClass().getCanonicalName());

            try {
                field.set(service, toInject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        int port = -1;
        String[] services = new String[0];

        for (String arg : args) {
            int equals = arg.indexOf('=');

            if (equals != -1) {
                String key = arg.substring(0, equals);
                String value = arg.substring(equals + 1);

                switch (key) {
                    case "--port":
                        port = Integer.parseInt(value);
                        break;
                    case "--services":
                        services = value.split(";");
                        break;
                    default:
                        break;
                }
            }
        }

        new Server(services)
                .connect(port);
    }

    public static Optional<Process> start(Class... services) {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = Server.class.getCanonicalName();
        String port = "--port=8454";
        String servicesArg = "--services=" + Arrays.stream(services)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(";"));

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className, port, servicesArg);

        try {
            return Optional.of(builder.start());
        } catch (IOException e) {
            System.err.println("Failed to start Server!");
            return Optional.empty();
        }
    }

    public Server connect(final int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        connectionListenerThread.start();

        return this;
    }

    private Server registerService(final String name) {
        final Service service = ReflectUtils.createInstance(
                name, Service.class.getCanonicalName(), new Class[]{Server.class}, this);

        services.add(service);
        servicesMap.put(name, service);

        System.out.println("Successfully loaded Service " + name);

        return this;
    }

    private void register(final Socket socket) {
        SocketConnection.create(Objects.requireNonNull(socket)).ifPresent(conn -> {
            conn.registerMessageCallback(this::messageReceived);
            conn.registerDisconnectCallback(this::connectionClosed);
            connections.put(conn.getHandle(), conn);
            System.out.printf("Registered connection with handle %d%n", conn.getHandle());
        });
    }

    public long registerMessageReceivedCallback(final BiConsumer<Long, String> callback) {
        Objects.requireNonNull(callback);
        return messageCallbacks.addCallback(message -> {
            callback.accept(message.getConnectionHandle(), message.getMessage());
            return null;
        });
    }

    private void messageReceived(final long handle, final String message) {
        TextMessage msg = new TextMessage(handle, message);
        messageCallbacks.callAll(msg);
    }

    public boolean sendMessage(final long handle, final String message) {
        Objects.requireNonNull(message);

        if (!connections.containsKey(handle)) return false;
        else connections.get(handle).sendMessage(message);
        return true;
    }

    public void broadcastMessage(final String message) {
        Objects.requireNonNull(message);

        connections.forEach((handle, conn) -> conn.sendMessage(message));
    }

    private void connectionClosed(final long handle) {
        System.out.printf("Client %d disconnected%n", handle);
        connections.remove(handle);
    }

}
