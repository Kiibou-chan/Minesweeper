package space.kiibou.gui

import space.kiibou.GApplet
import space.kiibou.event.MouseEventAction.*
import space.kiibou.event.MouseEventButton.LEFT
import space.kiibou.event.options
import space.kiibou.gui.BorderStyle.IN
import space.kiibou.gui.BorderStyle.OUT
import java.util.*

class Button(app: GApplet) : GraphicsElement(app) {
    val border = BorderBox(app).also {
        it.xProp.bind(xProp)
        it.yProp.bind(yProp)
        widthProp.bind(it.widthProp)
        heightProp.bind(it.heightProp)
        it.borderStyle = OUT
        super.addChild(it)
    }

    init {
        registerCallback(options(LEFT, PRESS)) { border.borderStyle = IN }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_ENTER))) { border.borderStyle = IN }
        registerCallback(options(LEFT, RELEASE)) { border.borderStyle = OUT }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_EXIT))) { border.borderStyle = OUT }
    }

    override fun addChild(element: GraphicsElement) {
        border.addChild(element)
        border.bindProps(element)
    }

    override fun removeChild(index: Int): GraphicsElement {
        return border.removeChild(index)
    }

}