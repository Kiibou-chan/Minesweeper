package space.kiibou.event

import java.util.*

enum class EventModifier {
    SHIFT, CTRL, META, ALT;

    companion object {
        fun fromProcessingEvent(event: processing.event.Event): EnumSet<EventModifier> {
            val modifiers = EnumSet.noneOf(EventModifier::class.java)
            if (event.isAltDown) modifiers.add(ALT)
            if (event.isControlDown) modifiers.add(CTRL)
            if (event.isMetaDown) modifiers.add(META)
            if (event.isShiftDown) modifiers.add(SHIFT)
            return modifiers
        }
    }
}