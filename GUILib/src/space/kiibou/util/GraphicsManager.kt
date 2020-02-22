package space.kiibou.util

import space.kiibou.GApplet
import space.kiibou.gui.GGraphics
import space.kiibou.gui.GraphicsElement
import java.util.*

class GraphicsManager {
    private lateinit var app: GApplet
    private val elements: MutableList<GraphicsElement> = ArrayList()

    fun pre() {
        elements.forEach(GraphicsElement::preInit)
        elements.forEach(GraphicsElement::init)
        elements.forEach(GraphicsElement::postInit)
        app.unregisterMethod("pre", this)
        app.gg = app.graphics as GGraphics
    }

    fun draw() = elements.forEach(GraphicsElement::draw)

    fun registerGraphicsElement(element: GraphicsElement) {
        elements.add(element)
    }

    fun registerApp(app: GApplet) {
        this.app = app
        app.registerMethod("pre", this)
        app.registerMethod("draw", this)
    }
}