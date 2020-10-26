package space.kiibou.util

import javafx.beans.property.SimpleIntegerProperty
import space.kiibou.GApplet
import space.kiibou.gui.GGraphics
import space.kiibou.gui.GraphicsElement
import java.util.*

class GraphicsManager {
    private lateinit var app: GApplet
    private val elements: MutableList<GraphicsElement> = ArrayList()

    val mouseX = SimpleIntegerProperty(0)
    val mouseY = SimpleIntegerProperty(0)
    val scaleProp = SimpleIntegerProperty(1)
    var scale
        get() = scaleProp.value
        set(value) {
            scaleProp.value = value
        }

    fun pre() {
        elements.forEach(GraphicsElement::init)
        app.unregisterMethod("pre", this)
        app.gg = app.graphics as GGraphics
    }

    fun draw() {
        mouseX.value = app.mouseX
        mouseY.value = app.mouseY
        elements.forEach(GraphicsElement::draw)
    }

    fun registerGraphicsElement(element: GraphicsElement) {
        elements.add(element)
        element.scaleProp.bind(scaleProp)
    }

    fun registerApp(app: GApplet) {
        this.app = app
        app.registerMethod("pre", this)
        app.registerMethod("draw", this)
    }
}

