package space.kiibou

import processing.core.PApplet
import space.kiibou.event.EventDispatcher
import space.kiibou.gui.GGraphics
import space.kiibou.gui.GraphicsElement
import space.kiibou.util.GraphicsManager

open class GApplet : PApplet() {
    lateinit var gg: GGraphics
    private val graphicsManager: GraphicsManager = GraphicsManager().apply { registerApp(this@GApplet) }
    private val eventDispatcher: EventDispatcher = EventDispatcher().apply { registerApp(this@GApplet) }

    fun registerMethod(methodName: String, target: GraphicsElement) =
            eventDispatcher.registerMethod(methodName, target)

    fun unregisterMethod(methodName: String, target: GraphicsElement) =
            eventDispatcher.unregisterMethod(methodName, target)

    fun registerGraphicsElement(element: GraphicsElement) = graphicsManager.registerGraphicsElement(element)

}