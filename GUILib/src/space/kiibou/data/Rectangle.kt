package space.kiibou.data

import javafx.beans.property.SimpleIntegerProperty

open class Rectangle(x: Int, y: Int, width: Int, height: Int) {
    val xProp = SimpleIntegerProperty(null, "Rectangle X-Property", x)
    open var x: Int
        get() = xProp.value
        set(value) {
            xProp.set(value)
        }

    val yProp = SimpleIntegerProperty(null, "Rectangle Y-Property", y)
    open var y: Int
        get() = yProp.value
        set(value) {
            yProp.set(value)
        }

    val widthProp = SimpleIntegerProperty(null, "${this.toString()} Width-Property", width)
    open var width: Int
        get() = widthProp.value
        set(value) {
            widthProp.set(value)
        }

    val heightProp = SimpleIntegerProperty(null, "${this.toString()} Height-Property", height)
    open var height: Int
        get() = heightProp.value
        set(value) {
            heightProp.set(value)
        }

    fun collides(px: Int, py: Int): Boolean {
        return px >= x && px < x + width && py >= y && py < y + height
    }
}
