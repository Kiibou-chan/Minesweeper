package com.kiibou

import processing.data.JSONObject
import space.kiibou.GApplet
import space.kiibou.event.MouseEventAction
import space.kiibou.event.MouseEventButton
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Picture
import space.kiibou.net.client.Client

class ControlBar(app: GApplet, scale: Int, private val map: Map) : GraphicsElement(app, 0, 0, 0, 0, scale) {
    private val client: Client = (app as Minesweeper).client
    lateinit var timerDisplay: SevenSegmentDisplay
    private lateinit var restartButton: Button
    lateinit var bombsLeft: SevenSegmentDisplay

    private lateinit var smileys: Array<Picture>

    override var width: Int
        get() = super.width
        set(value) {
            super.width = value
            val timerX = x + width - timerDisplay.width
            timerDisplay.moveTo(timerX, timerDisplay.y)
            val buttonX = x + width / 2 - restartButton.width / 2
            restartButton.moveTo(buttonX, restartButton.y)
        }

    override var height: Int
        get() = super.height
        set(value) {
            super.height = value
            val buttonY = y + height - restartButton.height
            restartButton.moveTo(restartButton.x, buttonY)
        }

    override fun preInitImpl() {
        bombsLeft = SevenSegmentDisplay(app, scale, 3, map.bombs)
        bombsLeft.setLowerLimit(0)
        addChild(bombsLeft)

        val smileyMap = Picture(app, "pictures/smiley.png", scale)
        restartButton = Button(app, scale)

        smileys = arrayOf(
                smileyMap.subPicture(0, 0, 20, 20),
                smileyMap.subPicture(20, 0, 20, 20),
                smileyMap.subPicture(40, 0, 20, 20),
                smileyMap.subPicture(60, 0, 20, 20)
        )

        restartButton.border.addChild(smileys[0])
        restartButton.border.addChild(smileys[1])
        restartButton.border.addChild(smileys[2])
        restartButton.border.addChild(smileys[3])

        restartButton.registerCallback(options(MouseEventButton.LEFT, MouseEventAction.RELEASE)) {
            client.sendJSON(JSONObject().setString("action", "restart"))
        }

        addChild(restartButton)
        setSmiley(SmileyStatus.NORMAL)
        timerDisplay = SevenSegmentDisplay(app, scale, 3, 0)
        addChild(timerDisplay)
    }

    override fun initImpl() {
        height = children.map { obj: GraphicsElement -> obj.height }.max() ?: 0
    }

    override fun postInitImpl() {}
    override fun drawImpl() {}

    fun setSmiley(status: SmileyStatus) {
        smileys.forEach { it.hide() }
        smileys[status.ordinal].show()
    }

}