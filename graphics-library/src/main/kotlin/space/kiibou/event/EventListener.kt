package space.kiibou.event

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

inline fun <T> ((T) -> Unit).andThen(crossinline other: (T) -> Unit) = { it: T ->
    this(it)
    other(it)
}
