package space.kiibou.event;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.function.Consumer;

public interface MouseEventListener {

    static MouseEventOption options(MouseEventButton button, MouseEventAction action, MouseEventModifier... modifiers) {
        return options(button, EnumSet.of(action), modifiers);
    }

    static MouseEventOption options(MouseEventButton button, EnumSet<MouseEventAction> actions, MouseEventModifier... modifiers) {
        final EnumSet<MouseEventModifier> mods = EnumSet.noneOf(MouseEventModifier.class);
        mods.addAll(Arrays.asList((modifiers)));
        return new MouseEventOption(button, actions, mods);
    }

    default void mouseEvent(MouseEvent event) {
        if (isActive()) {
            MouseOptionMap map = getMouseOptionMap();

            MouseEventOption option = event.getOption();

            map.computeIfPresent(option, (mouseEventOption, mouseEventConsumer) -> {
                mouseEventConsumer.accept(event);
                return mouseEventConsumer;
            });
        }
    }

    boolean isActive();

    void setActive(boolean active);

    default void activate() {
        if (!isActive()) {
            setActive(true);
        }
    }

    default void deactivate() {
        if (isActive()) {
            setActive(false);
        }
    }

    MouseOptionMap getMouseOptionMap();

    default void registerCallback(MouseEventOption option, Consumer<MouseEvent> callback) {
        getMouseOptionMap().merge(option, callback, Consumer::andThen);
    }

    default void unregisterCallback(MouseEventOption option) {
        getMouseOptionMap().remove(option);
    }

    class MouseOptionMap extends HashMap<MouseEventOption, Consumer<MouseEvent>> {
    }

}
