package space.kiibou.game

import space.kiibou.Minesweeper
import space.kiibou.common.TileInfo
import space.kiibou.gui.*

/**
 * The board view. Purely presentational: tile clicks send their own messages, and the
 * game-state messages are routed here by [Minesweeper] (registered once, forwarded to
 * the current map), so maps can be created per game without duplicating handlers.
 */
class Map(override val app: Minesweeper, private val tilesX: Int, private val tilesY: Int, val bombs: Int) :
    GraphicsElement(app) {
    private val margin = tileWidth / 4
    private val marginProp = scaleProperty.multiply(margin)

    private val box = BorderBox(app).also {
        it.style = BorderStyle.OUT
        addChild(it)
    }

    private val tiles = Grid<Tile>(app, tilesX, tilesY).also {
        (0 until tilesX).forEach { x ->
            (0 until tilesY).forEach { y ->
                it[x, y] = Tile(app, this, x, y)
            }
        }
    }

    private val tilesBox = BorderBox(app).also {
        it.addChild(tiles)
    }

    val controlBar = ControlBar(app, margin, this).also {
        it.widthProp.bind(tiles.widthProp)
    }

    private val controlBarBox = BorderBox(app).also {
        it.addChild(controlBar)
    }

    private val verticalList = VerticalList(app, margin).also {
        it.xProp.bind(box.xProp.add(box.borderWidthProp).add(marginProp))
        it.yProp.bind(box.yProp.add(box.borderHeightProp).add(marginProp))
        box.innerWidthProp.bind(it.widthProp.add(marginProp.multiply(2)))
        box.innerHeightProp.bind(it.heightProp.add(marginProp.multiply(2)))

        it.addChild(controlBarBox)
        it.addChild(tilesBox)
    }

    init {
        box.addChild(verticalList)

        widthProp.bind(box.widthProp)
        heightProp.bind(box.heightProp)
    }

    fun revealTiles(infos: List<TileInfo>) {
        infos.forEach { (x, y, type) ->
            tiles[x, y]!!.type = type
            tiles[x, y]!!.revealed = true
        }
    }

    fun setFlag(x: Int, y: Int, flagged: Boolean) {
        tiles[x, y]!!.flagged = flagged
    }

    fun onWin() {
        tiles.forEach(Tile::deactivate)
        controlBar.setSmiley(SmileyStatus.GLASSES)
    }

    fun onLose() {
        tiles.forEach(Tile::deactivate)
        controlBar.setSmiley(SmileyStatus.DEAD)
    }

    fun onRestart() {
        tiles.forEach(Tile::reset)
        controlBar.setSmiley(SmileyStatus.NORMAL)
    }

    override fun move(dx: Int, dy: Int): GraphicsElement {
        box.move(dx, dy)
        return super.move(dx, dy)
    }
}
