package space.kiibou.gui.vector

import space.kiibou.GApplet
import space.kiibou.data.Vec2
import space.kiibou.gui.GraphicsElement

class LineData : GraphicsElementFactory {
    lateinit var start: Vec2
    lateinit var end: Vec2
    lateinit var style: Style
    var scale = 1

    override fun invoke(app: GApplet): GraphicsElement {
        return Line(app, this)
    }

    override fun toString(): String {
        return "LineData{" +
                "start=" + start +
                ", end=" + end +
                ", style=" + style +
                ", scale=" + scale +
                '}'
    }
}