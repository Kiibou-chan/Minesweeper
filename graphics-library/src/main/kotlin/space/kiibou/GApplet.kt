package space.kiibou

import processing.core.PApplet
import space.kiibou.event.EventDispatcher
import space.kiibou.gui.text.TextMetrics
import space.kiibou.gui.GGraphicsOpenGL
import space.kiibou.gui.GraphicsElement
import space.kiibou.util.GraphicsManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class GApplet : PApplet() {
    val gg: GGraphicsOpenGL get() = g as GGraphicsOpenGL

    val eventDispatcher: EventDispatcher = EventDispatcher().also { it.registerApp(this) }
    protected val graphicsManager: GraphicsManager = GraphicsManager().also { it.registerApp(this) }

    /** Non-null switches text sizing to a headless implementation (see [TextMetrics]). */
    var textMetrics: TextMetrics? = null

    /** Set by test harnesses: exit() disposes the sketch instead of killing the JVM. */
    var suppressSystemExit: Boolean = false

    private val windowResizes: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "GApplet-window-resize").also { it.isDaemon = true }
    }

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

    /**
     * Resizes the window off the draw callback. Calling `surface.setSize` inside `draw`
     * makes NEWT apply the reshape inline, which re-enters `handleDraw`; Processing
     * answers that re-entry with `System.exit(1)`.
     */
    fun requestWindowSize(width: Int, height: Int) {
        windowResizes.execute { applyWindowSize(width, height) }
    }

    protected open fun applyWindowSize(width: Int, height: Int) = surface.setSize(width, height)

    companion object {
        val G2D: String = GGraphicsOpenGL::class.java.name
    }
}
