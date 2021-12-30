package space.kiibou.game

import space.kiibou.GApplet
import space.kiibou.Minesweeper
import space.kiibou.SevenSegmentDisplay
import space.kiibou.common.MinesweeperAction
import space.kiibou.event.MouseAction
import space.kiibou.event.MouseButton
import space.kiibou.event.options
import space.kiibou.game.SmileyStatus.NORMAL
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
        it.xProp.bind(xProp.add(scaleProperty.multiply(margin)))
        it.yProp.bind(yProp.add(scaleProperty.multiply(margin)))
        it.setLowerLimit(0)
        addChild(it)
    }

    private val restartButton = Button(app).also {
        it.xProp.bind(xProp.add(widthProp.divide(2).subtract(it.widthProp.divide(2))))
        it.yProp.bind(yProp.add(heightProp.divide(2).subtract(it.heightProp.divide(2))))
        it.addChild(smileys[NORMAL.ordinal])
        addChild(it)

        it.registerCallback(options(MouseButton.LEFT, MouseAction.RELEASE)) {
            (app as Minesweeper).client.send(MinesweeperAction.Restart)
        }
    }

    val timerDisplay: SevenSegmentDisplay = SevenSegmentDisplay(app, 3, 0).also {
        it.xProp.bind(xProp.add(widthProp).subtract(it.widthProp).subtract(scaleProperty.multiply(margin)))
        it.yProp.bind(yProp.add(scaleProperty.multiply(margin)))
        addChild(it)
    }

    init {
        heightProp.bind(scaleProperty.multiply(margin * 2).add(bombsLeft.heightProp))
    }

    fun setSmiley(status: SmileyStatus) {
        restartButton.border.removeChild(0)
        restartButton.border.addChild(smileys[status.ordinal])
        restartButton.border.bindProps(smileys[status.ordinal])
    }

    fun setTime(action: MinesweeperAction.SetTime) {
        timerDisplay.value = action.data.time
    }

    fun setBombsLeft(action: MinesweeperAction.SetBombsLeft) {
        bombsLeft.value = action.data
    }
}
