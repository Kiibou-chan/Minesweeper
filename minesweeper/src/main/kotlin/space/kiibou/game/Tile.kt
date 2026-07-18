package space.kiibou.game

import space.kiibou.Minesweeper
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.common.TilePosition
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
class Tile(override val app: Minesweeper, private val map: Map, private val tileX: Int, private val tileY: Int) :
    GraphicsElement(app) {

    var type: TileType = TileType.EMPTY
        set(value) {
            field = value

            deferAfterDraw {
                val index = getChildIndex(tilePicture)
                removeChildAt(index)
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

            // Only the cover button toggles: the tile itself stays active so a click on
            // a revealed number can request a chord.
            if (revealed) {
                button.hide()
                flagged = false
                button.deactivate()
            } else {
                button.show()
                flagged = false
                button.activate()
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

    private val flag: Picture = Picture(app, "tiles/flag_tile.png").also {
        it.hide()
    }

    private val button = Button(app, child = flag).also {
        it.xProp.bind(xProp)
        it.yProp.bind(yProp)
        addChild(it)
    }

    init {
        widthProp.bind(scaleProperty.multiply(tileWidth))
        heightProp.bind(scaleProperty.multiply(tileHeight))

        /* Reveal the tile, if possible, and set the smiley back to normal */
        button.registerCallback(options(LEFT, RELEASE)) {
            map.controlBar.setSmiley(SmileyStatus.NORMAL)
            app.client.send(
                MinesweeperMessageType.RevealTile,
                TilePosition(tileX, tileY)
            )
        }

        /* Flag the tile */
        button.registerCallback(options(RIGHT, RELEASE)) {
            app.client.send(
                MinesweeperMessageType.ToggleFlag,
                TilePosition(tileX, tileY)
            )
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

        /* Chord: clicking a revealed number tile asks the server to reveal its neighbors */
        registerCallback(options(LEFT, RELEASE)) {
            if (revealed) {
                app.client.send(
                    MinesweeperMessageType.RevealTile,
                    TilePosition(tileX, tileY)
                )
            }
        }
    }

    fun reset() {
        type = TileType.EMPTY
        revealed = false
        button.border.style = BorderStyle.OUT
        activate()
    }
}

const val tileWidth = 16
const val tileHeight = 16
