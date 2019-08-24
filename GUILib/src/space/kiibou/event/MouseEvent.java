package space.kiibou.event;

import java.util.EnumSet;

public class MouseEvent {
    private final processing.event.MouseEvent source;

    private final MouseEventButton button;
    private final EnumSet<MouseEventAction> actions;
    private final EnumSet<MouseEventModifier> modifiers;

    MouseEvent(processing.event.MouseEvent source) {
        this.source = source;
        button = MouseEventButton.fromProcessingEvent(source);
        actions = MouseEventAction.fromProcessingEvent(source);
        modifiers = MouseEventModifier.fromProcessingEvent(source);
    }

    MouseEvent(MouseEvent source, MouseEventAction action) {
        this.source = source.getSource();
        button = source.button;
        actions = source.actions.clone();
        actions.add(action);
        modifiers = source.modifiers.clone();
    }

    public MouseEventOption getOption() {
        return new MouseEventOption(button, actions, modifiers);
    }

    public processing.event.MouseEvent getSource() {
        return source;
    }

    public MouseEventButton getButton() {
        return button;
    }

    public EnumSet<MouseEventAction> getActions() {
        return actions;
    }

    public EnumSet<MouseEventModifier> getModifiers() {
        return modifiers;
    }

    public int getX() {
        return source.getX();
    }

    public int getY() {
        return source.getY();
    }

    public int getCount() {
        return source.getCount();
    }

    public long getMillis() {
        return source.getMillis();
    }
}
