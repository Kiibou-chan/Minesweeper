package space.kiibou.event;

import processing.event.MouseEvent;

import java.util.EnumSet;

public enum MouseEventModifier {
    SHIFT, CTRL, META, ALT;

    public static EnumSet<MouseEventModifier> fromProcessingEvent(MouseEvent event) {
        EnumSet<MouseEventModifier> modifiers = EnumSet.noneOf(MouseEventModifier.class);
        if (event.isAltDown()) modifiers.add(ALT);
        if (event.isControlDown()) modifiers.add(CTRL);
        if (event.isMetaDown()) modifiers.add(META);
        if (event.isShiftDown()) modifiers.add(SHIFT);
        return modifiers;
    }
}
