package space.kiibou.event

import java.util.*

class MouseEvent {
    private val source: processing.event.MouseEvent
    private val button: MouseEventButton
    private val actions: EnumSet<MouseEventAction>
    private val modifiers: EnumSet<MouseEventModifier>

    internal constructor(source: processing.event.MouseEvent) {
        this.source = source
        button = MouseEventButton.fromProcessingEvent(source)
        actions = MouseEventAction.fromProcessingEvent(source)
        modifiers = MouseEventModifier.fromProcessingEvent(source)
    }

    internal constructor(source: MouseEvent, action: MouseEventAction) {
        this.source = source.source
        button = source.button
        actions = source.actions.clone()
        actions.add(action)
        modifiers = source.modifiers.clone()
    }

    val option: MouseEventOption
        get() = MouseEventOption(button, actions, modifiers)

    val x: Int
        get() = source.x

    val y: Int
        get() = source.y

    val count: Int
        get() = source.count

    val millis: Long
        get() = source.millis

    override fun toString(): String {
        return "MouseEvent(source=$source, button=$button, actions=$actions, modifiers=$modifiers)"
    }

}