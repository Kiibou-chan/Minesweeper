package space.kiibou.gui.vector

import space.kiibou.GApplet
import space.kiibou.data.Vec2
import space.kiibou.gui.GraphicsElement

class RectangleData : GraphicsElementFactory {
    lateinit var start: Vec2
    lateinit var dim: Vec2
    lateinit var style: Style
    var scale = 1

    override fun invoke(app: GApplet): GraphicsElement {
        return Rectangle(app, this)
    }

    override fun toString(): String {
        return "RectangleData{" +
                "start=" + start +
                ", dim=" + dim +
                ", style=" + style +
                ", scale=" + scale +
                '}'
    }
}