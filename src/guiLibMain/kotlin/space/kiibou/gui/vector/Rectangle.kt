package space.kiibou.gui.vector

import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement

class Rectangle(app: GApplet, private val data: RectangleData) : GraphicsElement(app, data.start.x, data.start.y, data.dim.x, data.dim.y) {
    override fun drawImpl() {
        with(app.graphics) {
            pushStyle()

            if (!data.style.noFill) {
                fill(data.style.fill.toInt())
            } else {
                noFill()
            }

            if (!data.style.noStroke) {
                stroke(data.style.stroke.toInt())
                strokeWeight(data.style.strokeWeight.toFloat())
            } else {
                noStroke()
            }

            rect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
            popStyle()
        }
    }

}