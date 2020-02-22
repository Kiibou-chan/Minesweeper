package space.kiibou.gui

open class Rectangle(open var x: Int, open var y: Int, open var width: Int, open var height: Int) {
    fun collides(px: Int, py: Int): Boolean {
        return px >= x && px < x + width && py >= y && py < y + height
    }
}