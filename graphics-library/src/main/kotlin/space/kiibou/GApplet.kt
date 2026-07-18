package space.kiibou

import processing.core.PApplet
import space.kiibou.event.EventDispatcher
import space.kiibou.gui.text.TextMetrics
import space.kiibou.gui.GGraphicsOpenGL
import space.kiibou.gui.GraphicsElement
import space.kiibou.util.GraphicsManager

open class GApplet : PApplet() {
    val gg: GGraphicsOpenGL get() = g as GGraphicsOpenGL

    val eventDispatcher: EventDispatcher = EventDispatcher().also { it.registerApp(this) }
    protected val graphicsManager: GraphicsManager = GraphicsManager().also { it.registerApp(this) }

    /** Non-null switches text sizing to a headless implementation (see [TextMetrics]). */
    var textMetrics: TextMetrics? = null

    /** Set by test harnesses: exit() disposes the sketch instead of killing the JVM. */
    var suppressSystemExit: Boolean = false

    override fun exitActual() {
        if (!suppressSystemExit) super.exitActual()
    }

    /** Read-only view of the registered top-level elements, for scene queries. */
    val elementRoots: List<GraphicsElement> get() = graphicsManager.roots

    fun registerMethod(methodName: String, target: GraphicsElement) =
        eventDispatcher.registerMethod(methodName, target)

    fun unregisterMethod(methodName: String, target: GraphicsElement) =
        eventDispatcher.unregisterMethod(methodName, target)

    fun registerGraphicsElement(element: GraphicsElement) = graphicsManager.registerGraphicsElement(element)

    fun unregisterGraphicsElement(element: GraphicsElement) = graphicsManager.unregisterGraphicsElement(element)

    fun setScale(scale: Int) {
        graphicsManager.scale = scale
    }

    companion object {
        val G2D: String = GGraphicsOpenGL::class.java.name
    }
}
