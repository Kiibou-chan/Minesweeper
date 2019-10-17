package space.kiibou.net.server;

import java.util.Objects;

public abstract class Service {

    protected final Server server;

    public Service(Server server) {
        this.server = Objects.requireNonNull(server);
    }

    public abstract void initialize();

}
