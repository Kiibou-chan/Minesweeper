package space.kiibou.util

import javafx.beans.property.SimpleIntegerProperty
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement

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

    private var initialized = false

    @Suppress("unused")
    fun pre() {
        elements.forEach(GraphicsElement::init)
        initialized = true
        app.unregisterMethod("pre", this)
    }

    @Suppress("unused")
    fun draw() {
        mouseX.value = app.mouseX
        mouseY.value = app.mouseY
        elements.forEach(GraphicsElement::draw)
    }

    fun registerGraphicsElement(element: GraphicsElement) {
        elements.add(element)
        element.scaleProperty.bind(scaleProp)
        if (initialized) element.init()
    }

    fun registerApp(app: GApplet) {
        this.app = app
        app.registerMethod("pre", this)
        app.registerMethod("draw", this)
    }
}
