package space.kiibou.event;

import processing.event.MouseEvent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MouseEventAction {
    PRESS(processing.event.MouseEvent.PRESS),
    RELEASE(processing.event.MouseEvent.RELEASE),
    CLICK(processing.event.MouseEvent.CLICK),
    DRAG(processing.event.MouseEvent.DRAG),
    MOVE(processing.event.MouseEvent.MOVE),
    WINDOW_ENTER(processing.event.MouseEvent.ENTER),
    WINDOW_EXIT(processing.event.MouseEvent.EXIT),
    WHEEL(processing.event.MouseEvent.WHEEL),
    ELEMENT_ENTER(-1),
    ELEMENT_EXIT(-2);

    private static final Map<Integer, MouseEventAction> mapper = new HashMap<>();

    static {
        for (MouseEventAction action : MouseEventAction.values()) {
            if (action.id >= 0)
                mapper.put(action.id, action);
        }
    }

    private int id;

    MouseEventAction(int id) {
        this.id = id;
    }

    public static EnumSet<MouseEventAction> fromProcessingEvent(MouseEvent event) {
        return EnumSet.of(mapper.get(event.getAction()));
    }
}
