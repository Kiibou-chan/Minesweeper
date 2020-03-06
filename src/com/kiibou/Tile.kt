package com.kiibou

import space.kiibou.GApplet
import space.kiibou.event.MouseEventAction.*
import space.kiibou.event.MouseEventButton.LEFT
import space.kiibou.event.MouseEventButton.RIGHT
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Picture
import java.util.*

/**
 * @param tileX X-Position of the tile on the Map
 * @param tileY Y-Position of the tile on the Map
 */
class Tile(app: GApplet, private val map: Map, private val tileX: Int, private val tileY: Int)
    : GraphicsElement(app) {

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
    }

    override fun preInitImpl() {}
    override fun initImpl() {
        /* GUI stuff */

        /* Button and Mouse Stuff */
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

    override fun postInitImpl() {}
    public override fun drawImpl() {}

    private fun reveal() {
        (app as Minesweeper).client.sendJson(mapper.createObjectNode()
                .put("action", "reveal-tiles")
                .put("x", tileX)
                .put("y", tileY))
    }

    private fun flag() {
        (app as Minesweeper).client.sendJson(mapper.createObjectNode()
                .put("action", "flag-toggle")
                .put("x", tileX)
                .put("y", tileY))
    }

    fun reset() {
        type = TileType.EMPTY
        revealed = false
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