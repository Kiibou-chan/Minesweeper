package space.kiibou.event

import java.util.*

class MouseOptionMap : HashMap<MouseEventOption, (MouseEvent) -> Unit>()
typealias MouseEventConsumer = (MouseEvent) -> Unit

interface MouseEventListener {
    fun mouseEvent(event: MouseEvent) {
        if (active) mouseOptionMap[event.option]?.invoke(event)
    }

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

fun options(button: MouseEventButton, action: MouseEventAction, vararg modifiers: MouseEventModifier): MouseEventOption {
    return options(button, EnumSet.of(action), *modifiers)
}

fun options(button: MouseEventButton, actions: EnumSet<MouseEventAction>, vararg modifiers: MouseEventModifier): MouseEventOption {
    val mods = EnumSet.noneOf(MouseEventModifier::class.java)
    mods.addAll(listOf(*modifiers))
    return MouseEventOption(button, actions, mods)
}