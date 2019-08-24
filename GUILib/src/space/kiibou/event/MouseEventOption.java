package space.kiibou.event;

import java.util.EnumSet;
import java.util.Objects;

public final class MouseEventOption {
    private final MouseEventButton button;
    private final EnumSet<MouseEventAction> action;
    private final EnumSet<MouseEventModifier> modifiers;

    MouseEventOption(MouseEventButton button, EnumSet<MouseEventAction> action, EnumSet<MouseEventModifier> modifiers) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MouseEventOption that = (MouseEventOption) o;

        if (button != that.button) return false;
        if (!Objects.equals(action, that.action)) return false;
        return Objects.equals(modifiers, that.modifiers);
    }

    @Override
    public int hashCode() {
        int result = button != null ? button.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (modifiers != null ? modifiers.hashCode() : 0);
        return result;
    }
}
