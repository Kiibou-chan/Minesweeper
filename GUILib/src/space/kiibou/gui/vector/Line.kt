package space.kiibou.gui.vector

import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement
import kotlin.math.min

class Line(app: GApplet, private val data: LineData)
    : GraphicsElement(app, min(data.start.x, data.end.x), min(data.start.y, data.end.y), min(data.start.x, data.end.x), min(data.start.y, data.end.y)) {

    override fun preInitImpl() {}
    override fun initImpl() {}
    override fun postInitImpl() {}

    override fun drawImpl() {
        app.gg.scaled(scale) {
            if (!data.style.noStroke) {
                it.stroke(data.style.stroke.toInt())
                it.strokeWeight(data.style.strokeWeight.toFloat())
            } else {
                it.noStroke()
            }
            it.line(data.start, data.end)
        }
    }

}