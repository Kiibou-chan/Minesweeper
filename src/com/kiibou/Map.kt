package com.kiibou

import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Grid
import space.kiibou.gui.VerticalList
import java.util.function.Consumer

class Map(app: GApplet, x: Int, y: Int, private val tilesX: Int, private val tilesY: Int, scale: Int, val bombs: Int) : GraphicsElement(app, x, y, tilesX * Tile.tileHeight * scale, tilesY * Tile.tileHeight * scale, scale) {
    private lateinit var tiles: Grid<Tile>
    private lateinit var verticalList: VerticalList
    lateinit var controlBar: ControlBar
        private set

    public override fun preInitImpl() {
        verticalList = VerticalList(app, x, y, scale)
        addChild(verticalList)
        controlBar = ControlBar(app, scale, this)
        verticalList.addChild(controlBar)
        tiles = createGrid(tilesX, tilesY)
        verticalList.addChild(tiles)
    }

    public override fun initImpl() {
        controlBar.width = verticalList.getChild(0).innerWidth
        resize(verticalList.width, verticalList.height)
    }

    public override fun postInitImpl() {}
    public override fun drawImpl() {}

    private fun createGrid(tilesX: Int, tilesY: Int): Grid<Tile> {
        val tiles = Grid<Tile>(app, 0, 0, tilesX, tilesY, scale)

        (0 until tilesX).forEach { x ->
            (0 until tilesY).forEach { y ->
                tiles.put(x, y, Tile(app, this, scale, x, y))
            }
        }

        return tiles
    }

    fun revealTile(x: Int, y: Int, type: TileType) {
        tiles[x, y].type = type
        tiles[x, y].revealed = true
    }

    fun win() {
        controlBar.setSmiley(SmileyStatus.GLASSES)
        forEachTile(Consumer { obj: Tile -> obj.deactivate() })
    }

    fun loose() {
        forEachTile(Consumer { obj: Tile -> obj.deactivate() })
        controlBar.setSmiley(SmileyStatus.DEAD)
    }

    fun restart() {
        forEachTile(Consumer { obj: Tile -> obj.reset() })
        controlBar.setSmiley(SmileyStatus.NORMAL)
        controlBar.bombsLeft.value = bombs
    }

    fun tileFlag(x: Int, y: Int, flagged: Boolean) {
        when (flagged) {
            true -> controlBar.bombsLeft.apply { value = value-- }
            false -> controlBar.bombsLeft.apply { value = value++ }
        }

        tiles[x, y].flagged = flagged
    }

    private fun forEachTile(action: Consumer<Tile>) {
        tiles.forEach(action)
    }

}