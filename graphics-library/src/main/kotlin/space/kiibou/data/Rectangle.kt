package space.kiibou.data

import javafx.beans.property.SimpleIntegerProperty

open class Rectangle(x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) {
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

    val widthProp = SimpleIntegerProperty(null, "Width-Property", width)
    open var width: Int
        get() = widthProp.value
        set(value) {
            widthProp.set(value)
        }

    val heightProp = SimpleIntegerProperty(null, "Height-Property", height)
    open var height: Int
        get() = heightProp.value
        set(value) {
            heightProp.set(value)
        }

    fun collides(px: Int, py: Int): Boolean {
        return px in x..x + width && py in y..y + height
    }
}
