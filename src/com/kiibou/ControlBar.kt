package com.kiibou

import com.kiibou.SmileyStatus.NORMAL
import space.kiibou.GApplet
import space.kiibou.event.MouseEventAction
import space.kiibou.event.MouseEventButton
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Picture

class ControlBar(app: GApplet, margin: Int, map: Map) : GraphicsElement(app) {
    private val smileys: Array<Picture> = Picture(app, "pictures/smiley.png").let {
        arrayOf(
                it.subPicture(0, 0, 20, 20),
                it.subPicture(20, 0, 20, 20),
                it.subPicture(40, 0, 20, 20),
                it.subPicture(60, 0, 20, 20)
        )
    }

    val bombsLeft = SevenSegmentDisplay(app, 3, map.bombs).also {
        it.xProp.bind(xProp.add(scaleProp.multiply(margin)))
        it.yProp.bind(yProp.add(scaleProp.multiply(margin)))
        it.setLowerLimit(0)
        addChild(it)
    }

    private val restartButton = Button(app).also {
        it.xProp.bind(xProp.add(widthProp.divide(2).subtract(it.widthProp.divide(2))))
        it.yProp.bind(yProp.add(heightProp.divide(2).subtract(it.heightProp.divide(2))))
        it.addChild(smileys[NORMAL.ordinal])
        addChild(it)

        it.registerCallback(options(MouseEventButton.LEFT, MouseEventAction.RELEASE)) {
            (app as Minesweeper).client.sendJson(mapper.createObjectNode().put("action", "restart"))
        }
    }

    val timerDisplay: SevenSegmentDisplay = SevenSegmentDisplay(app, 3, 0).also {
        it.xProp.bind(xProp.add(widthProp).subtract(it.widthProp).subtract(scaleProp.multiply(margin)))
        it.yProp.bind(yProp.add(scaleProp.multiply(margin)))
        addChild(it)
    }

    init {
        heightProp.bind(scaleProp.multiply(margin * 2).add(bombsLeft.heightProp))
    }

    fun setSmiley(status: SmileyStatus) {
        restartButton.border.removeChild(0)
        restartButton.border.addChild(smileys[status.ordinal])
        restartButton.border.bindProps(smileys[status.ordinal])
    }

}