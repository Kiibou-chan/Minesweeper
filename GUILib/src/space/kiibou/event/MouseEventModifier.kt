package space.kiibou.event

import processing.event.MouseEvent
import java.util.*

enum class MouseEventModifier {
    SHIFT, CTRL, META, ALT;

    companion object {
        fun fromProcessingEvent(event: MouseEvent): EnumSet<MouseEventModifier> {
            val modifiers = EnumSet.noneOf(MouseEventModifier::class.java)
            if (event.isAltDown) modifiers.add(ALT)
            if (event.isControlDown) modifiers.add(CTRL)
            if (event.isMetaDown) modifiers.add(META)
            if (event.isShiftDown) modifiers.add(SHIFT)
            return modifiers
        }
    }
}