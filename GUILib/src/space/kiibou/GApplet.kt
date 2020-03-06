package space.kiibou

import processing.core.PApplet
import space.kiibou.event.EventDispatcher
import space.kiibou.gui.GGraphics
import space.kiibou.gui.GraphicsElement
import space.kiibou.util.GraphicsManager

open class GApplet : PApplet() {
    lateinit var gg: GGraphics
    protected val eventDispatcher: EventDispatcher = EventDispatcher().also { it.registerApp(this) }
    protected val graphicsManager: GraphicsManager = GraphicsManager().also { it.registerApp(this) }

    fun registerMethod(methodName: String, target: GraphicsElement) =
            eventDispatcher.registerMethod(methodName, target)

    fun unregisterMethod(methodName: String, target: GraphicsElement) =
            eventDispatcher.unregisterMethod(methodName, target)

    fun registerGraphicsElement(element: GraphicsElement) = graphicsManager.registerGraphicsElement(element)

    fun setScale(scale: Int) {
        graphicsManager.scale = scale
    }

}