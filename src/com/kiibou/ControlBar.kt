package com.kiibou

import com.kiibou.SmileyStatus.NORMAL
import javafx.beans.binding.Bindings
import processing.data.JSONObject
import space.kiibou.GApplet
import space.kiibou.event.MouseEventAction
import space.kiibou.event.MouseEventButton
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Picture

class ControlBar(app: GApplet, scale: Int, map: Map) : GraphicsElement(app, 0, 0, 0, 0, scale) {
    private val smileys: Array<Picture> = Picture(app, "pictures/smiley.png", scale).let {
        arrayOf(
                it.subPicture(0, 0, 20, 20),
                it.subPicture(20, 0, 20, 20),
                it.subPicture(40, 0, 20, 20),
                it.subPicture(60, 0, 20, 20)
        )
    }

    val bombsLeft = SevenSegmentDisplay(app, scale, 3, map.bombs).also {
        it.xProp.bind(xProp)
        it.yProp.bind(yProp)
        it.setLowerLimit(0)
        addChild(it)
    }

    val restartButton = Button(app, scale).also {
        it.xProp.bind(widthProp.divide(2).subtract(it.widthProp.divide(2)))
        it.yProp.bind(yProp)
        it.addChild(smileys[NORMAL.ordinal])
        addChild(it)

        it.registerCallback(options(MouseEventButton.LEFT, MouseEventAction.RELEASE)) {
            (app as Minesweeper).client.sendJSON(JSONObject().setString("action", "restart"))
        }
    }

    val timerDisplay: SevenSegmentDisplay = SevenSegmentDisplay(app, scale, 3, 0).also {
        it.xProp.bind(xProp.add(widthProp).subtract(it.widthProp))
        it.yProp.bind(yProp)
        addChild(it)
    }

    init {
        heightProp.bind(Bindings.max(timerDisplay.heightProp, Bindings.max(restartButton.heightProp, bombsLeft.heightProp)))
    }

    override fun preInitImpl() {}
    override fun initImpl() {}
    override fun postInitImpl() {}
    override fun drawImpl() {}

    fun setSmiley(status: SmileyStatus) {
        restartButton.border.removeChild(0)
        restartButton.border.addChild(smileys[status.ordinal])
        restartButton.border.bindProps(smileys[status.ordinal])
    }

}