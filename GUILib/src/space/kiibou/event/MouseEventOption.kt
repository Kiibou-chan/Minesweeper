package space.kiibou.event

import java.util.*

data class MouseEventOption internal constructor(private val button: MouseEventButton, private val action: EnumSet<MouseEventAction>, private val modifiers: EnumSet<MouseEventModifier>)