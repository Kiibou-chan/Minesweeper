package space.kiibou.gui.vector

import space.kiibou.GApplet
import space.kiibou.data.Vec2
import space.kiibou.gui.GraphicsElement

class VertexData : GraphicsElementFactory {
    var beginShape = Constants.NONE
    var endShape = Constants.NONE
    lateinit var vertices: List<Vec2>
    lateinit var start: Vec2
    lateinit var dim: Vec2
    lateinit var style: Style
    var scale = 1

    override fun invoke(app: GApplet): GraphicsElement {
        return Vertex(app, this)
    }

    override fun toString(): String {
        return "VertexData{" +
                "beginShape='" + beginShape + '\'' +
                ", endShape='" + endShape + '\'' +
                ", vertices=" + vertices +
                ", start=" + start +
                ", dim=" + dim +
                ", style=" + style +
                '}'
    }
}