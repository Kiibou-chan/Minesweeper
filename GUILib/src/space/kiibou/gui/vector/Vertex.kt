package space.kiibou.gui.vector

import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement

class Vertex(app: GApplet?, private val data: VertexData) : GraphicsElement(app!!, data.start.x, data.start.y, data.dim.x, data.dim.y, data.scale) {
    override fun preInitImpl() {}
    override fun initImpl() {}
    override fun postInitImpl() {}

    override fun drawImpl() {
        with(app.graphics) {
            pushStyle()

            if (!data.style.noStroke) {
                stroke(data.style.stroke.toInt())
                strokeWeight(data.style.strokeWeight.toFloat())
            } else noStroke()

            if (!data.style.noFill) fill(data.style.fill.toInt()) else noFill()
            // TODO: 03/11/2019 strokeJoin with MITER, BEVEL, ROUND
            // TODO: 03/11/2019 strokeCap with ROUND, SQUARE, PROJECT
            if (data.beginShape !== Constants.NONE) beginShape(data.beginShape.kind) else beginShape()
            for ((x1, y1) in data.vertices) vertex(x1 + data.start.x.toFloat(), y1 + data.start.y.toFloat())
            if (data.endShape !== Constants.NONE) endShape(data.endShape.kind) else endShape()
            popStyle()
        }
    }

}