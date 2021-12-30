package space.kiibou.gui

import processing.core.PFont
import processing.opengl.PGraphicsOpenGL
import space.kiibou.data.Color
import space.kiibou.data.Vec2

class GGraphicsOpenGL : PGraphicsOpenGL() {
    private val scalarStack: ArrayDeque<Int> = ArrayDeque(initialCapacity = 32)

    fun scaled(scale: Int, action: (GGraphicsOpenGL) -> Unit) {
        scalarStack.addLast(scale)
        action(this)
        scalarStack.removeLast()
    }

    fun line(start: Vec2, end: Vec2) {
        line(start.x.toFloat(), start.y.toFloat(), end.x.toFloat(), end.y.toFloat())
    }

    override fun lineImpl(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
        val scalar = getCurrentScalar()
        super.lineImpl(x1 * scalar, y1 * scalar, z1 * scalar, x2 * scalar, y2 * scalar, z2 * scalar)
    }

    override fun strokeWeight(weight: Float) {
        val scalar = getCurrentScalar()
        super.strokeWeight(weight * scalar)
    }

    fun fill(color: Color) {
        fill(color.value)
    }

    fun stroke(color: Color) {
        stroke(color.value)
    }

    private fun getCurrentScalar(): Int = if (scalarStack.size > 0) scalarStack.last() else 1

    fun textWidth(font: PFont, size: Int, text: String): Int {
        pushStyle()
        textFont(font)
        textSize(size.toFloat())
        val width = textWidth(text).toInt()
        popStyle()
        return width
    }

    fun textHeight(font: PFont, size: Int, text: String): Int {
        pushStyle()
        textFont(font)
        textSize(size.toFloat())
        val lineBreaks = text.count { it == '\n' }
        val height = (textAscent() + textDescent()) * (lineBreaks + 1)
        popStyle()
        return height.toInt()
    }
}
