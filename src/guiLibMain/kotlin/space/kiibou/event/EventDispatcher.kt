@file:Suppress("unused", "UNUSED_PARAMETER")

package space.kiibou.event

import processing.event.KeyEvent
import processing.event.TouchEvent
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement
import space.kiibou.net.common.Message
import space.kiibou.net.common.MessageType
import space.kiibou.net.common.Router
import java.util.*

class EventDispatcher {
    private lateinit var app: GApplet
    private val registry = mapOf(
        "keyEvent" to HashSet<GraphicsElement>(),
        "mouseEvent" to HashSet(),
        "touchEvent" to HashSet()
    )

    private val mouseQueue = Collections.synchronizedList(ArrayList<processing.event.MouseEvent>())
    private val keyQueue = Collections.synchronizedList(ArrayList<KeyEvent>())
    private val messageQueue = Collections.synchronizedList(ArrayList<Message<*>>())

    private val router = Router()

    private var prevGraphicsElement: GraphicsElement? = null

    private fun topElement(x: Int, y: Int, elements: Set<GraphicsElement>): GraphicsElement? {
        return elements
            .filter { it.collides(x, y) }
            .maxByOrNull(GraphicsElement::hierarchyDepth)
    }

    private fun dispatchEvents() {
        synchronized(mouseQueue) {
            mouseQueue.forEach {
                val event = MouseEvent(it)
                val topElement = topElement(event.x, event.y, registry["mouseEvent"]!!)

                if (topElement != null) {
                    val sameElement = topElement == prevGraphicsElement
                    if (!sameElement) {
                        if (prevGraphicsElement != null) {
                            prevGraphicsElement!!.mouseEvent(MouseEvent(event, MouseAction.ELEMENT_EXIT))
                        }
                        topElement.mouseEvent(MouseEvent(event, MouseAction.ELEMENT_ENTER))
                        prevGraphicsElement = topElement
                    }
                    topElement.mouseEvent(event)
                }

                if (topElement == null && prevGraphicsElement != null) {
                    prevGraphicsElement!!.mouseEvent(MouseEvent(event, MouseAction.ELEMENT_EXIT))
                    prevGraphicsElement = null
                }
            }
            mouseQueue.clear()
        }

        synchronized(messageQueue) {
            messageQueue.forEach(router::messageReceived)
            messageQueue.clear()
        }
    }

    fun pre() {
        dispatchEvents()
    }

    fun keyEvent(event: KeyEvent) {}

    fun mouseEvent(source: processing.event.MouseEvent) {
        if (source.button == 0) return

        synchronized(mouseQueue) {
            mouseQueue += source
        }
    }

    fun touchEvent(event: TouchEvent) {}

    fun messageEvent(obj: Message<*>) {
        synchronized(messageQueue) {
            messageQueue.add(obj)
        }
    }

    fun <T : Any> onMessage(type: MessageType<T>, callback: (Message<T>) -> Unit) =
        router.addCallback(type, callback)

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
