package space.kiibou.data

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import space.kiibou.reactive.map
import space.kiibou.reactive.now
import space.kiibou.reactive.observe
import space.kiibou.reactive.reactives.DynVar
import space.kiibou.reactive.reactives.Signal
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DynVarRWProperty<T : Any>(private val dynVar: DynVar<T>) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = dynVar.now

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = dynVar.set(value)
}

val <T : Any> DynVar<T>.prop: DynVarRWProperty<T>
    get() = DynVarRWProperty(this)

operator fun Signal<Int>.plus(other: Int): Signal<Int> = this.map { it + other }
operator fun Int.plus(other: Signal<Int>): Signal<Int> = other.map { it + this@plus }
operator fun Signal<Int>.minus(other: Int): Signal<Int> = this.map { it - other }
operator fun Int.minus(other: Signal<Int>): Signal<Int> = other.map { it - this@minus }
operator fun Signal<Int>.times(other: Int): Signal<Int> = this.map { it * other }
operator fun Int.times(other: Signal<Int>): Signal<Int> = other.map { it * this@times }
operator fun Signal<Int>.div(other: Int): Signal<Int> = this.map { it / other }
operator fun Int.div(other: Signal<Int>): Signal<Int> = other.map { it / this@div }

infix fun Signal<Int>.max(other: Signal<Int>): Signal<Int> =
    Signal.static(this, other) { maxOf(this@max.value, other.value) }

fun max(vararg signals: Signal<Int>): Signal<Int> = Signal.static(*signals) {
    signals.maxOfOrNull { it.value } ?: 0
}

val Signal<String>.toFX: SimpleStringProperty
    get() {
        val prop = SimpleStringProperty("")

        now?.also { prop.set(it) }
        observe { prop.set(it) }

        return prop
    }

val Signal<Int>.toFX: SimpleIntegerProperty
    get() {
        val prop = SimpleIntegerProperty(0)

        now?.also { prop.set(it) }
        observe { prop.set(it) }

        return prop
    }
