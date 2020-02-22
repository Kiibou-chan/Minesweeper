@file:Suppress("MapGetWithNotNullAssertionOperator", "unused")

package space.kiibou.event

import processing.event.KeyEvent
import processing.event.TouchEvent
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement
import java.util.*

class EventDispatcher {
    private lateinit var app: GApplet
    private val registry = mapOf(
            "keyEvent" to HashSet<GraphicsElement>(),
            "mouseEvent" to HashSet(),
            "touchEvent" to HashSet()
    )

    private var prevGraphicsElement: GraphicsElement? = null

    private fun topElement(x: Int, y: Int, elements: Set<GraphicsElement>): GraphicsElement? {
        return elements
                .filter { it.collides(x, y) }
                .maxBy { it.hierarchyDepth }
    }

    fun keyEvent(event: KeyEvent) {}

    fun mouseEvent(source: processing.event.MouseEvent) {
        if (source.button == 0) return

        val event = MouseEvent(source)
        val topElement = topElement(event.x, event.y, registry["mouseEvent"]!!)

        topElement?.let { element: GraphicsElement ->
            val sameElement = element == prevGraphicsElement
            if (!sameElement) {
                if (prevGraphicsElement != null) {
                    prevGraphicsElement!!.mouseEvent(MouseEvent(event, MouseEventAction.ELEMENT_EXIT))
                }
                element.mouseEvent(MouseEvent(event, MouseEventAction.ELEMENT_ENTER))
                prevGraphicsElement = element
            }
            element.mouseEvent(event)
        }

        if (topElement != null && prevGraphicsElement != null) {
            prevGraphicsElement!!.mouseEvent(MouseEvent(event, MouseEventAction.ELEMENT_EXIT))
            prevGraphicsElement = null
        }
    }

    fun touchEvent(event: TouchEvent) {}

    fun registerMethod(eventType: String, element: GraphicsElement) {
        when (eventType) {
            "keyEvent" -> registry["keyEvent"]!!.add(element)
            "mouseEvent" -> registry["mouseEvent"]!!.add(element)
            "touchEvent" -> registry["touchEvent"]!!.add(element)
            else -> app.registerMethod(eventType, element as Any)
        }
    }

    fun unregisterMethod(eventType: String, element: GraphicsElement) {
        when (eventType) {
            "keyEvent" -> registry["keyEvent"]!!.remove(element)
            "mouseEvent" -> registry["mouseEvent"]!!.remove(element)
            "touchEvent" -> registry["touchEvent"]!!.remove(element)
            else -> app.registerMethod(eventType, element as Any)
        }
    }

    fun registerApp(app: GApplet) {
        this.app = app
        app.registerMethod("keyEvent", this)
        app.registerMethod("mouseEvent", this)
        app.registerMethod("touchEvent", this)
    }
}