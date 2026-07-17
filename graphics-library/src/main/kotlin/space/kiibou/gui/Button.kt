package space.kiibou.gui

import javafx.scene.input.KeyCode
import mu.KotlinLogging
import space.kiibou.GApplet
import space.kiibou.event.Event
import space.kiibou.event.KeyAction
import space.kiibou.event.MouseAction.*
import space.kiibou.event.MouseButton.LEFT
import space.kiibou.event.MouseEvent
import space.kiibou.event.options
import space.kiibou.gui.BorderStyle.IN
import space.kiibou.gui.BorderStyle.OUT
import space.kiibou.reactive.reactives.Evt
import space.kiibou.reactive.reactives.fire
import java.util.*

private val logger = KotlinLogging.logger { }

// TODO (Svenja, 30/12/2021): Add secondary constructor to immediately pass the child element to the button.
//  Currently this usually happens with an `also` or `apply` call after instantiation.
class Button(app: GApplet) : GraphicsElement(app) {
    override var focusable: Boolean
        get() = active
        set(value) {}

    constructor(app: GApplet, child: GraphicsElement) : this(app) {
        this += child
    }

    val border = BorderBox(app).also {
        it.xProp.bind(xProp)
        it.yProp.bind(yProp)
        widthProp.bind(it.widthProp)
        heightProp.bind(it.heightProp)
        it.style = OUT
        super.addChild(it)
    }

    val clicked: Evt<Event> = Evt()

    init {
        registerCallback(options(LEFT, PRESS)) { border.style = IN }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_ENTER))) { border.style = IN }
        registerCallback(options(LEFT, RELEASE)) { border.style = OUT }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_EXIT))) { border.style = OUT }

        registerCallback(options(LEFT, RELEASE)) { clicked.fire(it) }

        registerCallback(options(KeyCode.ENTER, KeyAction.RELEASE)) {
            logger.info { "KEY EVT: $it" }
            clicked.fire(it)
        }
    }

    override fun addChild(element: GraphicsElement) {
        border.addChild(element)
    }

    override fun removeChildAt(index: Int): GraphicsElement {
        return border.removeChildAt(index)
    }
}
