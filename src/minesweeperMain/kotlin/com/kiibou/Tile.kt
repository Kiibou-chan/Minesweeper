package com.kiibou

import com.kiibou.common.MinesweeperAction
import space.kiibou.GApplet
import space.kiibou.data.Vec2
import space.kiibou.event.MouseAction.*
import space.kiibou.event.MouseButton.LEFT
import space.kiibou.event.MouseButton.RIGHT
import space.kiibou.event.options
import space.kiibou.gui.BorderStyle
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Picture
import java.util.*

/**
 * @param tileX X-Position of the tile on the Map
 * @param tileY Y-Position of the tile on the Map
 */
class Tile(app: GApplet, private val map: Map, private val tileX: Int, private val tileY: Int) : GraphicsElement(app) {

    var type: TileType = TileType.EMPTY
        set(value) {
            field = value

            deferAfterDraw {
                val index = getChildIndex(tilePicture)
                removeChild(tilePicture)
                tilePicture.xProp.unbind()
                tilePicture.yProp.unbind()
                tilePicture.widthProp.unbind()
                tilePicture.heightProp.unbind()

                tilePicture = Picture(app, value.path)
                tilePicture.xProp.bind(xProp)
                tilePicture.yProp.bind(yProp)
                tilePicture.widthProp.bind(widthProp)
                tilePicture.heightProp.bind(heightProp)

                addChild(index, tilePicture)
            }
        }

    var revealed: Boolean = false
        set(value) {
            field = value

            if (revealed) {
                button.hide()
                flagged = false
                deactivate()
            } else {
                button.show()
                flagged = false
                activate()
            }
        }

    var flagged: Boolean = false
        set(value) {
            if (value) flag.show() else flag.hide()
            field = value
        }

    private var tilePicture: Picture = Picture(app, type.path).also {
        it.xProp.bind(xProp)
        it.yProp.bind(yProp)
        it.widthProp.bind(widthProp)
        it.heightProp.bind(heightProp)
        addChild(it)
    }

    private val button = Button(app).also {
        it.xProp.bind(xProp)
        it.yProp.bind(yProp)
        addChild(it)
    }

    private val flag: Picture = Picture(app, "tiles/flag_tile.png").also {
        button.addChild(it)
        it.hide()
    }

    init {
        widthProp.bind(scaleProp.multiply(tileWidth))
        heightProp.bind(scaleProp.multiply(tileHeight))

        /* Reveal the tile, if possible, and set the smiley back to normal */
        button.registerCallback(options(LEFT, RELEASE)) {
            map.controlBar.setSmiley(SmileyStatus.NORMAL)
            reveal()
        }

        /* Flag the tile */
        button.registerCallback(options(RIGHT, RELEASE)) {
            flag()
        }

        /* Set smiley to surprised */
        button.registerCallback(options(LEFT, PRESS)) {
            map.controlBar.setSmiley(SmileyStatus.SURPRISED)
        }

        /* Same as above */
        button.registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_ENTER))) {
            map.controlBar.setSmiley(SmileyStatus.SURPRISED)
        }

        /* Set Smiley to normal */
        button.registerCallback(options(LEFT, EnumSet.of(DRAG, ELEMENT_EXIT))) {
            map.controlBar.setSmiley(SmileyStatus.NORMAL)
        }
    }

    private fun reveal() {
        (app as Minesweeper).client.send(
            MinesweeperAction.RevealTile(Vec2(tileX, tileY))
        )
    }

    private fun flag() {
        (app as Minesweeper).client.send(
            MinesweeperAction.ToggleFlag(Vec2(tileX, tileY))
        )
    }

    fun reset() {
        type = TileType.EMPTY
        revealed = false
        button.border.borderStyle = BorderStyle.OUT
    }

    override fun activate() {
        button.activate()
    }

    override fun deactivate() {
        button.deactivate()
    }

}

const val tileWidth = 16
const val tileHeight = 16
