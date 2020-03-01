@file:Suppress("MapGetWithNotNullAssertionOperator", "unused")

package space.kiibou.event

import com.fasterxml.jackson.databind.JsonNode
import processing.event.KeyEvent
import processing.event.TouchEvent
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement
import space.kiibou.net.common.ActionDispatcher
import java.util.*

class EventDispatcher {
    private lateinit var app: GApplet
    private val registry = mapOf(
            "keyEvent" to HashSet<GraphicsElement>(),
            "mouseEvent" to HashSet(),
            "touchEvent" to HashSet()
    )

    private val mouseQueue = Collections.synchronizedList(ArrayList<processing.event.MouseEvent>())
    private val jsonQueue = Collections.synchronizedList(ArrayList<JsonNode>())

    private fun dispatchJson(action: String, obj: JsonNode) {
        jsonDispatcher.dispatchAction(action, obj)
    }

    private val jsonDispatcher = ActionDispatcher<JsonNode> {
        if (it.has("action")) {
            val action = it.get("action").textValue()
            dispatchJson(action, it)
        }
    }

    private var prevGraphicsElement: GraphicsElement? = null

    private fun topElement(x: Int, y: Int, elements: Set<GraphicsElement>): GraphicsElement? {
        return elements
                .filter { it.collides(x, y) }
                .maxBy { it.hierarchyDepth }
    }

    private fun dispatchEvents() {
        mouseQueue.forEach {
            val event = MouseEvent(it)
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
        mouseQueue.clear()

        jsonQueue.forEach {
            jsonDispatcher.messageReceived(it)
        }
        jsonQueue.clear()
    }

    fun pre() {
        dispatchEvents()
    }

    fun keyEvent(event: KeyEvent) {}

    fun mouseEvent(source: processing.event.MouseEvent) {
        if (source.button == 0) return

        mouseQueue += source
    }

    fun touchEvent(event: TouchEvent) {}

    fun jsonEvent(obj: JsonNode) {
        jsonQueue.add(obj)
    }

    fun registerJsonCallback(action: String, callback: (JsonNode) -> Unit) {
        jsonDispatcher.addActionCallback(action, callback)
    }

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
        app.registerMethod("pre", this)
        app.registerMethod("keyEvent", this)
        app.registerMethod("mouseEvent", this)
        app.registerMethod("touchEvent", this)
    }
}