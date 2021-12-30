package space.kiibou.gui

import space.kiibou.GApplet
import space.kiibou.event.MouseAction.*
import space.kiibou.event.MouseButton.LEFT
import space.kiibou.event.options
import space.kiibou.gui.BorderStyle.IN
import space.kiibou.gui.BorderStyle.OUT
import java.util.*

// TODO (Svenja, 30/12/2021): Add secondary constructor to immediately pass the child element to the button.
//  Currently this usually happens with an `also` or `apply` call after instantiation.
class Button(app: GApplet) : GraphicsElement(app) {
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

    init {
        registerCallback(options(LEFT, PRESS)) { border.style = IN }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_ENTER))) { border.style = IN }
        registerCallback(options(LEFT, RELEASE)) { border.style = OUT }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_EXIT))) { border.style = OUT }
    }

    override fun addChild(element: GraphicsElement) {
        border.addChild(element)
        border.bindProps(element)
    }

    override fun removeChild(index: Int): GraphicsElement {
        return border.removeChild(index)
    }
}
