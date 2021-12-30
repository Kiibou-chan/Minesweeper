package space.kiibou.game

import space.kiibou.Minesweeper
import space.kiibou.common.MapInfo
import space.kiibou.common.MinesweeperAction
import space.kiibou.gui.*

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
        it.bindProps(tiles)
    }

    val controlBar = ControlBar(app, margin, this).also {
        it.widthProp.bind(tiles.widthProp)
    }

    private val controlBarBox = BorderBox(app).also {
        it.addChild(controlBar)
        it.bindProps(controlBar)
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

    override fun initImpl() {
        app.apply {
            registerActionCallback<MinesweeperAction.SetTime> {
                controlBar.timerDisplay.value = it.data.time
            }

            registerActionCallback<MinesweeperAction.RevealTiles> {
                it.data.tiles.forEach { (x, y, type) ->
                    tiles[x, y]!!.type = type
                    tiles[x, y]!!.revealed = true
                }
            }

            registerActionCallback<MinesweeperAction.Win> {
                tiles.forEach(Tile::deactivate)
                controlBar.setSmiley(SmileyStatus.GLASSES)
            }

            registerActionCallback<MinesweeperAction.Loose> {
                tiles.forEach(Tile::deactivate)
                controlBar.setSmiley(SmileyStatus.DEAD)
            }

            registerActionCallback<MinesweeperAction.Restart> {
                tiles.forEach(Tile::reset)
                controlBar.setSmiley(SmileyStatus.NORMAL)
            }

            registerActionCallback<MinesweeperAction.SetFlag> {
                val (x, y, status) = it.data

                tiles[x, y]!!.flagged = status
            }

            registerActionCallback<MinesweeperAction.SetBombsLeft> {
                controlBar.bombsLeft.value = it.data
            }

            client.send(MinesweeperAction.InitMap(MapInfo(tilesX, tilesY, bombs)))
        }
    }

    override fun move(dx: Int, dy: Int): GraphicsElement {
        box.move(dx, dy)
        return super.move(dx, dy)
    }
}
