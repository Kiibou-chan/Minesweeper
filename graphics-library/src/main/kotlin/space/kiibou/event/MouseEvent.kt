package space.kiibou.event

import processing.core.PConstants
import java.util.*

class MouseEvent {
    private val source: processing.event.MouseEvent
    private val button: MouseButton
    private val actions: EnumSet<MouseAction>
    private val modifiers: EnumSet<EventModifier>

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

    val option: MouseEventOption get() = MouseEventOption(button, actions, modifiers)
    val x: Int get() = source.x
    val y: Int get() = source.y
    val count: Int get() = source.count
    val millis: Long get() = source.millis
    override fun toString() = "MouseEvent(source=$source, button=$button, actions=$actions, modifiers=$modifiers)"
}

class KeyEvent {
//    private val source: processing.event.KeyEvent
}

data class MouseEventOption internal constructor(private val button: MouseButton, private val action: EnumSet<MouseAction>, private val modifiers: EnumSet<EventModifier>)

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

class MouseOptionMap : HashMap<MouseEventOption, (MouseEvent) -> Unit>()
typealias MouseEventConsumer = (MouseEvent) -> Unit

interface EventListener {
    var active: Boolean
    fun activate() {
        if (!active) {
            active = true
        }
    }

    fun deactivate() {
        if (active) {
            active = false
        }
    }
}

interface MouseEventListener : EventListener {
    fun mouseEvent(event: MouseEvent) {
        if (active) mouseOptionMap[event.option]?.invoke(event)
    }

    val mouseOptionMap: MouseOptionMap
    fun registerCallback(option: MouseEventOption, callback: MouseEventConsumer) {
        mouseOptionMap.merge(option, callback) { obj, after -> obj.andThen(after) }
    }

    fun unregisterCallback(option: MouseEventOption) {
        mouseOptionMap.remove(option)
    }
}

inline fun <T> ((T) -> Unit).andThen(crossinline other: (T) -> Unit) = { it: T ->
    this(it)
    other(it)
}

fun options(button: MouseButton, action: MouseAction, vararg modifiers: EventModifier): MouseEventOption {
    return options(button, EnumSet.of(action), *modifiers)
}

fun options(button: MouseButton, actions: EnumSet<MouseAction>, vararg modifiers: EventModifier): MouseEventOption {
    val mods = EnumSet.noneOf(EventModifier::class.java)
    mods.addAll(listOf(*modifiers))
    return MouseEventOption(button, actions, mods)
}

enum class MouseButton(private val id: Int) {
    LEFT(PConstants.LEFT),
    RIGHT(PConstants.RIGHT),
    CENTER(PConstants.CENTER);

    companion object {
        private val MAPPER: Map<Int, MouseButton> = values().associateBy { it.id }

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
        private val MAPPER: Map<Int, MouseAction> = values().filter { it.id > 0 }.associateBy { it.id }

        fun fromProcessingEvent(event: processing.event.MouseEvent): EnumSet<MouseAction> = EnumSet.of(MAPPER[event.action])
    }
}
