package space.kiibou.game

import space.kiibou.Minesweeper
import space.kiibou.common.MapInfo
import space.kiibou.common.MinesweeperAction
import space.kiibou.common.MinesweeperAction.Loose
import space.kiibou.common.MinesweeperAction.Restart
import space.kiibou.common.MinesweeperAction.RevealTiles
import space.kiibou.common.MinesweeperAction.SetBombsLeft
import space.kiibou.common.MinesweeperAction.SetFlag
import space.kiibou.common.MinesweeperAction.SetTime
import space.kiibou.common.MinesweeperAction.Win
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

    override fun initImpl() {
        with(app) {
            onAction<SetTime> {
                controlBar.timerDisplay.value = it.data.time
            }

            onAction<RevealTiles> {
                it.data.tiles.forEach { (x, y, type) ->
                    tiles[x, y]!!.type = type
                    tiles[x, y]!!.revealed = true
                }
            }

            onAction<Win> {
                tiles.forEach(Tile::deactivate)
                controlBar.setSmiley(SmileyStatus.GLASSES)
            }

            onAction<Loose> {
                tiles.forEach(Tile::deactivate)
                controlBar.setSmiley(SmileyStatus.DEAD)
            }

            onAction<Restart> {
                tiles.forEach(Tile::reset)
                controlBar.setSmiley(SmileyStatus.NORMAL)
            }

            onAction<SetFlag> {
                val (x, y, status) = it.data

                tiles[x, y]!!.flagged = status
            }

            onAction<SetBombsLeft> {
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
