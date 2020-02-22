package space.kiibou.gui

import space.kiibou.GApplet
import space.kiibou.event.MouseEventAction.*
import space.kiibou.event.MouseEventButton.LEFT
import space.kiibou.event.options
import space.kiibou.gui.BorderStyle.IN
import space.kiibou.gui.BorderStyle.OUT
import java.util.*

class Button(app: GApplet, scale: Int) : GraphicsElement(app, 0, 0, 0, 0, scale) {
    val border = BorderBox(app, scale).apply(::addChild)

    override fun preInitImpl() {}
    override fun initImpl() {
        border.moveTo(x, y)

        if (border.width > 0 && border.height > 0) {
            resize(border.width, border.height)
        } else {
            border.resize(width, height)
        }

        border.borderStyle = OUT

        registerCallback(options(LEFT, PRESS)) { border.borderStyle = IN }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_ENTER))) { border.borderStyle = IN }
        registerCallback(options(LEFT, RELEASE)) { border.borderStyle = OUT }
        registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_EXIT))) { border.borderStyle = OUT }
    }

    override fun postInitImpl() {}
    override fun drawImpl() {}

}