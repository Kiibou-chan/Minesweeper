package space.kiibou.test

import processing.core.PConstants
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement

/** Depth-first traversal of an element and all its descendants. */
fun GraphicsElement.walk(): Sequence<GraphicsElement> = sequence {
    yield(this@walk)
    children.toList().forEach { yieldAll(it.walk()) }
}

/**
 * Drives a headless [GApplet] the way a user would: clicks are synthesized at a widget's
 * real position and delivered through the production [space.kiibou.event.EventDispatcher]
 * (hit-testing, focus, and hidden/inactive filtering included), typing goes to whatever
 * the click focused. Pair with `GApplet.textMetrics = EstimatingTextMetrics` so layout
 * computes without a sketch.
 */
class GuiRobot(private val app: GApplet) {

    /** Drains all queued events, like one frame boundary. */
    fun pump() = app.eventDispatcher.pre()

    fun findByTagOrNull(tag: String): GraphicsElement? =
        app.elementRoots.asSequence().flatMap { it.walk() }.find { it.testTag == tag }

    fun findByTag(tag: String): GraphicsElement =
        findByTagOrNull(tag) ?: error(
            "No element tagged '$tag'. Known tags: " +
                app.elementRoots.flatMap { root -> root.walk().mapNotNull { it.testTag }.toList() },
        )

    fun clickOn(tag: String) = clickOn(findByTag(tag))

    fun clickOn(element: GraphicsElement) = click(element, PConstants.LEFT)

    fun rightClickOn(tag: String) = click(findByTag(tag), PConstants.RIGHT)

    fun type(text: String) {
        text.forEach { key(it) }
        pump()
    }

    fun pressEnter() = type("\n")

    private fun click(element: GraphicsElement, button: Int) {
        val cx = element.x + element.width / 2
        val cy = element.y + element.height / 2

        mouse(processing.event.MouseEvent.PRESS, cx, cy, button)
        mouse(processing.event.MouseEvent.RELEASE, cx, cy, button)
        pump()
    }

    private fun mouse(action: Int, x: Int, y: Int, button: Int) =
        app.eventDispatcher.mouseEvent(
            processing.event.MouseEvent(null, 0L, action, 0, x, y, button, 1),
        )

    private fun key(char: Char) =
        app.eventDispatcher.keyEvent(
            processing.event.KeyEvent(null, 0L, processing.event.KeyEvent.TYPE, 0, char, 0),
        )
}
