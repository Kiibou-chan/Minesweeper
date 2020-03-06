package com.kiibou

import space.kiibou.GApplet
import space.kiibou.gui.*

class Map(app: GApplet, private val tilesX: Int, private val tilesY: Int, val bombs: Int)
    : GraphicsElement(app) {

    private val margin = tileWidth / 4
    private val marginProp = scaleProp.multiply(margin)

    private val box = BorderBox(app).also {
        it.borderStyle = BorderStyle.OUT
        addChild(it)
    }

    private val verticalList = VerticalList(app, margin).also {
        box.addChild(it)
        it.xProp.bind(box.xProp.add(box.borderWidthProp).add(marginProp))
        it.yProp.bind(box.yProp.add(box.borderHeightProp).add(marginProp))
        box.innerWidthProp.bind(it.widthProp.add(marginProp.multiply(2)))
        box.innerHeightProp.bind(it.heightProp.add(marginProp.multiply(2)))
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
        it.bindProps(tiles)
    }

    val controlBar = ControlBar(app, margin, this).also {
        it.widthProp.bind(tiles.widthProp)
    }

    private val controlBarBox = BorderBox(app).also {
        it.addChild(controlBar)
        it.bindProps(controlBar)
    }

    init {
        verticalList += controlBarBox
        verticalList += tilesBox

        widthProp.bind(box.widthProp)
        heightProp.bind(box.heightProp)
    }

    public override fun preInitImpl() {}
    public override fun initImpl() {
        (app as Minesweeper).client.sendJson(mapper.valueToTree(MapInfo(tilesX, tilesY, bombs)))
    }

    public override fun postInitImpl() {}
    public override fun drawImpl() {}

    fun revealTile(x: Int, y: Int, type: TileType) {
        tiles[x, y]!!.type = type
        tiles[x, y]!!.revealed = true
    }

    fun revealTiles(revealTiles: RevealTiles) {
        revealTiles.tiles.forEach {
            revealTile(it.x, it.y, TileType.getTypeFromValue(it.type))
        }
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

    fun tileFlag(flagInfo: FlagInfo) {
        when (flagInfo.toggle) {
            true -> controlBar.bombsLeft.dec()
            false -> controlBar.bombsLeft.inc()
        }

        tiles[flagInfo.x, flagInfo.y]!!.flagged = flagInfo.toggle
    }

    private inline fun forEachTile(action: (Tile) -> Unit) {
        tiles.forEach(action)
    }

}