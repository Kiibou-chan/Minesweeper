package com.kiibou

import space.kiibou.GApplet
import space.kiibou.gui.BorderBox
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Grid
import space.kiibou.gui.VerticalList

class Map(app: GApplet, x: Int, y: Int, private val tilesX: Int, private val tilesY: Int, scale: Int, val bombs: Int)
    : GraphicsElement(app, x, y, tilesX * tileHeight * scale, tilesY * tileHeight * scale, scale) {
    private val verticalList = VerticalList(app, x, y, scale).also {
        addChild(it)
    }

    private val tiles = Grid<Tile>(app, 0, 0, tilesX, tilesY, scale).also {
        (0 until tilesX).forEach { x ->
            (0 until tilesY).forEach { y ->
                it[x, y] = Tile(app, this, scale, x, y)
            }
        }
    }
    private val tilesBox = BorderBox(app, scale).also {
        it.addChild(tiles)
        it.bindProps(tiles)
    }

    val controlBar = ControlBar(app, scale, this).also {
        it.widthProp.bind(tiles.widthProp)
    }
    private val controlBarBox = BorderBox(app, scale).also {
        it.addChild(controlBar)
        it.bindProps(controlBar)
    }

    init {
        verticalList += controlBarBox
        verticalList += tilesBox

        widthProp.bind(verticalList.widthProp)
        heightProp.bind(verticalList.heightProp)
    }

    public override fun preInitImpl() {}
    public override fun initImpl() {}
    public override fun postInitImpl() {}
    public override fun drawImpl() {}

    fun revealTile(x: Int, y: Int, type: TileType) {
        tiles[x, y]!!.type = type
        tiles[x, y]!!.revealed = true
    }

    fun win() {
        controlBar.setSmiley(SmileyStatus.GLASSES)
        forEachTile(Tile::deactivate)
    }

    fun loose() {
        forEachTile(Tile::deactivate)
        controlBar.setSmiley(SmileyStatus.DEAD)
    }

    fun restart() {
        forEachTile(Tile::reset)
        controlBar.setSmiley(SmileyStatus.NORMAL)
        controlBar.bombsLeft.value = bombs
    }

    fun tileFlag(x: Int, y: Int, flagged: Boolean) {
        when (flagged) {
            true -> controlBar.bombsLeft.dec()
            false -> controlBar.bombsLeft.inc()
        }

        tiles[x, y]!!.flagged = flagged
    }

    private inline fun forEachTile(action: (Tile) -> Unit) {
        tiles.forEach(action)
    }

}