package space.kiibou.net.common;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Callbacks<IN, OUT> {

    private static long handleCounter = 0;

    private final Map<Long, Function<IN, OUT>> callbacks;

    public Callbacks() {
        callbacks = Collections.synchronizedMap(new HashMap<>());
    }

    private static long nextHandle() {
        return handleCounter++;
    }

    public long addCallback(final Function<IN, OUT> function) {
        Objects.requireNonNull(function);

        long handle = nextHandle();
        callbacks.put(handle, function);
        return handle;
    }

    public Function<IN, OUT> removeCallback(final long handle) {
        return callbacks.remove(handle);
    }

    public OUT call(final long handle, final IN arg) {
        return callbacks.get(handle).apply(arg);
    }

    public List<OUT> callAll(final IN arg) {
        return callbacks.values().stream()
                .map(fun -> fun.apply(arg))
                .collect(Collectors.toList());
    }

}
