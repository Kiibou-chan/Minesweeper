package space.kiibou.event

import processing.core.PConstants
import java.util.*

class MouseEvent : Event {
    private val source: processing.event.MouseEvent
    val button: MouseButton
    val actions: EnumSet<MouseAction>
    val modifiers: EnumSet<EventModifier>

    internal constructor(source: processing.event.MouseEvent) {
        this.source = source
        button = MouseButton.fromProcessingEvent(source)
        actions = MouseAction.fromProcessingEvent(source)
        modifiers = EventModifier.fromProcessingEvent(source)
    }

    internal constructor(source: MouseEvent, action: MouseAction) {
        this.source = source.source
        button = source.button
        actions = source.actions.clone()
        actions.add(action)
        modifiers = source.modifiers.clone()
    }

    val option: MouseEventOption
        get() = MouseEventOption(button, actions, modifiers)

    /** The option a callback registered through [anyModifiers] is filed under. */
    val optionIgnoringModifiers: MouseEventOption
        get() = MouseEventOption(button, actions, null)

    val x: Int get() = source.x
    val y: Int get() = source.y
    val count: Int get() = source.count
    val millis: Long get() = source.millis
    override fun toString() = "MouseEvent(source=$source, button=$button, actions=$actions, modifiers=$modifiers)"
}

@ConsistentCopyVisibility
data class MouseEventOption internal constructor(
    private val button: MouseButton,
    private val action: EnumSet<MouseAction>,
    /** Null matches any combination of held modifiers. */
    private val modifiers: EnumSet<EventModifier>?
)

typealias MouseEventConsumer = (MouseEvent) -> Unit
typealias MouseOptionMap = HashMap<MouseEventOption, MouseEventConsumer>

interface MouseEventListener : EventListener {
    val mouseOptionMap: MouseOptionMap

    fun mouseEvent(event: MouseEvent) {
        if (!active) return

        val callback = mouseOptionMap[event.option] ?: mouseOptionMap[event.optionIgnoringModifiers]

        callback?.invoke(event)
    }

    fun registerCallback(option: MouseEventOption, callback: MouseEventConsumer) {
        mouseOptionMap.merge(option, callback) { obj, after -> obj.andThen(after) }
    }

    fun unregisterCallback(option: MouseEventOption) {
        mouseOptionMap.remove(option)
    }
}

fun options(button: MouseButton, action: MouseAction, vararg modifiers: EventModifier): MouseEventOption {
    return options(button, EnumSet.of(action), *modifiers)
}

fun options(button: MouseButton, actions: EnumSet<MouseAction>, vararg modifiers: EventModifier): MouseEventOption {
    val mods = EnumSet.noneOf(EventModifier::class.java)
    mods.addAll(listOf(*modifiers))
    return MouseEventOption(button, actions, mods)
}

/** Matches [button] and [action] whatever modifiers are held; see the key-event twin. */
fun anyModifiers(button: MouseButton, action: MouseAction): MouseEventOption =
    anyModifiers(button, EnumSet.of(action))

fun anyModifiers(button: MouseButton, actions: EnumSet<MouseAction>): MouseEventOption =
    MouseEventOption(button, actions, null)

enum class MouseButton(private val id: Int) {
    LEFT(PConstants.LEFT),
    RIGHT(PConstants.RIGHT),
    CENTER(PConstants.CENTER);

    companion object {
        private val MAPPER: Map<Int, MouseButton> = entries.associateBy { it.id }

        fun fromProcessingEvent(event: processing.event.MouseEvent): MouseButton {
            return MAPPER[event.button]
                    ?: error("Could not associate processingMouseButton:${event.button} with a MouseEventButton")
        }
    }

}

enum class MouseAction(private val id: Int) {
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

    companion object {
        private val MAPPER: Map<Int, MouseAction> = entries.filter { it.id > 0 }.associateBy { it.id }

        fun fromProcessingEvent(event: processing.event.MouseEvent): EnumSet<MouseAction> = EnumSet.of(MAPPER[event.action])
    }
}
