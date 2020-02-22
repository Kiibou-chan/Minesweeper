package space.kiibou.gui

import processing.opengl.PGraphicsOpenGL
import space.kiibou.data.Vec2
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

class GGraphics : PGraphicsOpenGL() {
    private val scalarStack: Deque<Int> = ConcurrentLinkedDeque()

    fun scaled(scale: Int, action: (GGraphics) -> Unit) {
        scalarStack.push(scale)
        action(this)
        scalarStack.pop()
    }

    fun line(start: Vec2, end: Vec2) {
        line(start.x.toFloat(), start.y.toFloat(), end.x.toFloat(), end.y.toFloat())
    }

    override fun lineImpl(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
        val scalar = scalar
        super.lineImpl(x1 * scalar, y1 * scalar, z1 * scalar, x2 * scalar, y2 * scalar, z2 * scalar)
    }

    override fun strokeWeight(weight: Float) {
        val scalar = scalar
        super.strokeWeight(weight * scalar)
    }

    private val scalar: Int
        get() = if (scalarStack.size > 0) scalarStack.peek() else 1
}