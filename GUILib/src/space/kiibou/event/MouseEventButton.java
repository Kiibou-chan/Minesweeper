package space.kiibou.event;

import processing.core.PConstants;

import java.util.HashMap;
import java.util.Map;

public enum MouseEventButton {
    LEFT(PConstants.LEFT),
    RIGHT(PConstants.RIGHT),
    CENTER(PConstants.CENTER);

    private static final Map<Integer, MouseEventButton> mapper = new HashMap<>();

    static {
        for (MouseEventButton button : MouseEventButton.values()) {
            mapper.put(button.id, button);
        }
    }

    private int id;

    MouseEventButton(int id) {
        this.id = id;
    }

    public static MouseEventButton fromProcessingEvent(processing.event.MouseEvent event) {
        return mapper.get(event.getButton());
    }
}
