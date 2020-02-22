package com.kiibou

import processing.data.JSONObject
import space.kiibou.GApplet
import space.kiibou.event.MouseEventAction.*
import space.kiibou.event.MouseEventButton.LEFT
import space.kiibou.event.MouseEventButton.RIGHT
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Picture
import space.kiibou.net.client.Client
import java.util.*

/**
 * @param tileX X-Position of the tile on the Map
 * @param tileY Y-Position of the tile on the Map
 */
class Tile(app: GApplet, private val map: Map, scale: Int, private val tileX: Int, private val tileY: Int)
    : GraphicsElement(app, 0, 0, scale * tileWidth, scale * tileHeight, scale) {

    companion object {
        const val tileWidth = 16
        const val tileHeight = 16
    }

    private val client: Client = (app as Minesweeper).client
    var type: TileType = TileType.EMPTY
        set(value) {
            field = value

            deferAfterDraw {
                val index = getChildIndex(tilePicture)
                removeChild(tilePicture)
                tilePicture = Picture(app, value.path, scale)
                addChild(index, tilePicture)
                tilePicture.moveTo(x, y)
                tilePicture.resize(width, height)
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
    private lateinit var button: Button
    private val flag: Picture = Picture(app, "tiles/flag_tile.png", scale).apply { hide() }
    private var tilePicture: Picture = Picture(app, type.path, scale)

    override fun preInitImpl() {
        addChild(tilePicture)
        button = Button(app, scale)
        button.resize(width, height)
        button.border.addChild(flag)
        addChild(button)
    }

    override fun initImpl() { /* GUI stuff */
        button.moveTo(x, y)
        tilePicture.resize(width, height)
        tilePicture.moveTo(x, y)
        flag.moveTo(button.x + button.border.borderWidth, button.y + button.border.borderHeight)
        flag.resize(button.border.innerWidth, button.border.innerHeight)

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
        client.sendJSON(JSONObject()
                .setString("action", "reveal-tiles")
                .setInt("x", tileX)
                .setInt("y", tileY)
        )
    }

    private fun flag() {
        client.sendJSON(JSONObject()
                .setString("action", "flag-toggle")
                .setInt("x", tileX)
                .setInt("y", tileY))
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