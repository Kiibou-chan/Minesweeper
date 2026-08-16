package space.kiibou.event

import javafx.scene.input.KeyCode
import java.util.*
import kotlin.collections.HashMap

private val keyCodesById: Map<Int, KeyCode> = KeyCode.entries.associateBy { it.code }

class KeyEvent internal constructor(private val source: processing.event.KeyEvent) : Event {
    val key: Char
        get() = source.key

    val keyCode: KeyCode = keyCodesById[source.keyCode] ?: KeyCode.UNDEFINED

    val isAutoRepeat: Boolean
        get() = source.isAutoRepeat

    val option: KeyEventOption
        get() = KeyEventOption(keyCode, action, modifier)

    /** The option a callback registered through [anyModifiers] is filed under. */
    val optionIgnoringModifiers: KeyEventOption
        get() = KeyEventOption(keyCode, action, null)

    val modifier: EnumSet<EventModifier> = EventModifier.fromProcessingEvent(source)
    val action: KeyAction = KeyAction.fromProcessingEvent(source)
    val millis: Long get() = source.millis

    override fun toString(): String {
        return "KeyEvent(source=$source, keyCode=$keyCode, isAutoRepeat=$isAutoRepeat, action=$action, millis=$millis)"
    }


}

enum class KeyAction(val id: Int) {
    PRESS(processing.event.KeyEvent.PRESS),
    RELEASE(processing.event.KeyEvent.RELEASE),
    TYPE(processing.event.KeyEvent.TYPE);

    companion object {
        private val MAPPER: Map<Int, KeyAction> = entries.associateBy { it.id }

        fun fromProcessingEvent(event: processing.event.KeyEvent): KeyAction {
            return MAPPER[event.action]
                ?: error("Could not associate KeyEvent action:${event.action} with a MouseEventButton")
        }
    }
}

@ConsistentCopyVisibility
data class KeyEventOption internal constructor(
    private val keyCode: KeyCode,
    private val action: KeyAction,
    /** Null matches any combination of held modifiers. */
    private val modifier: EnumSet<EventModifier>?
)

typealias KeyEventConsumer = (KeyEvent) -> Unit
typealias KeyOptionMap = HashMap<KeyEventOption, KeyEventConsumer>

interface KeyEventListener : EventListener {
    val keyOptionMap: KeyOptionMap

    fun keyEvent(event: KeyEvent) {
        if (!active) return

        val callback = keyOptionMap[event.option] ?: keyOptionMap[event.optionIgnoringModifiers]

        callback?.invoke(event)
    }

    fun registerCallback(option: KeyEventOption, callback: KeyEventConsumer) {
        keyOptionMap.merge(option, callback) { obj, after -> obj.andThen(after) }
    }

    fun unregisterCallback(option: KeyEventOption) {
        keyOptionMap.remove(option)
    }
}

fun options(key: KeyCode, action: KeyAction, vararg modifiers: EventModifier): KeyEventOption {
    return KeyEventOption(key, action, EnumSet.noneOf(EventModifier::class.java).apply { addAll(modifiers) })
}

/**
 * Matches [key] and [action] whatever modifiers are held. An [options] registration for
 * the same key and action takes precedence over this one for its own combination, which
 * is how a modifier gets special-cased without losing the default.
 */
fun anyModifiers(key: KeyCode, action: KeyAction): KeyEventOption = KeyEventOption(key, action, null)
