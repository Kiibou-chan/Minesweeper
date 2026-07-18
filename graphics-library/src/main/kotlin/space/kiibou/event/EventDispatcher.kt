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

    private val router: Router<Message<*>> = Router()

    /** Stores the last visited graphics element (through hovering the mouse) */
    private var lastHoveredElement: GraphicsElement? = null

    /**
     * Stores the currently focused element.
     * Elements can only be focused of they are focusable.
     *
     * Focus can be achieved through:
     * - Clicking the element (MouseAction.Press)
     * - Switching focus to the element though some other keyboard event
     */
    private var focusedElement: GraphicsElement? = null

    /** The element key events are currently dispatched to, if any. */
    val focused: GraphicsElement? get() = focusedElement

    private fun Collection<GraphicsElement>.topElement(x: Int, y: Int): GraphicsElement? {
        return filter { it.active && !it.effectivelyHidden && it.collides(x, y) }
            .maxByOrNull(GraphicsElement::hierarchyDepth)
    }

    private fun dispatchEvents() {
        synchronized(mouseQueue) {
            mouseQueue.forEach {
                val event = MouseEvent(it)
                val topElement = registry["mouseEvent"]?.topElement(event.x, event.y)

                if (topElement != null) {
                    val sameElement = topElement == lastHoveredElement
                    if (!sameElement) {
                        if (lastHoveredElement != null) {
                            lastHoveredElement!!.mouseEvent(MouseEvent(event, MouseAction.ELEMENT_EXIT))
                        }
                        topElement.mouseEvent(MouseEvent(event, MouseAction.ELEMENT_ENTER))
                        lastHoveredElement = topElement
                    }

                    topElement.mouseEvent(event)
                }

                if (MouseAction.PRESS in event.actions) {
                    registry["keyEvent"]?.filter(GraphicsElement::focusable)?.topElement(event.x, event.y)?.let {
                        focusedElement = it
                    }
                }

                if (topElement == null && lastHoveredElement != null) {
                    lastHoveredElement!!.mouseEvent(MouseEvent(event, MouseAction.ELEMENT_EXIT))
                    lastHoveredElement = null
                }
            }
            mouseQueue.clear()
        }

        synchronized(keyQueue) {
            keyQueue.forEach {
                val event = KeyEvent(it)

                focusedElement?.keyEvent(event)
            }

            keyQueue.clear()
        }

        synchronized(messageQueue) {
            messageQueue.forEach(router::messageReceived)
            messageQueue.clear()
        }
    }

    fun pre() {
        dispatchEvents()
    }

    /** True when no queued events are waiting to be dispatched. */
    fun isIdle(): Boolean =
        synchronized(mouseQueue) { mouseQueue.isEmpty() } &&
            synchronized(keyQueue) { keyQueue.isEmpty() } &&
            synchronized(messageQueue) { messageQueue.isEmpty() }

    fun keyEvent(event: KeyEvent) {
        synchronized(keyQueue) {
            keyQueue += event
        }
    }

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
