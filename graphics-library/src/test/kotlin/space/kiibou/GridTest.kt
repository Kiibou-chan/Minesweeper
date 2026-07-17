package space.kiibou

import processing.core.PApplet
import space.kiibou.gui.Button
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Grid
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.TextElement
import space.kiibou.reactive.now
import space.kiibou.reactive.observe
import space.kiibou.reactive.reactives.Events

class GridTest : GApplet() {
    override fun settings() {
        setScale(2)
        size(1200, 1200, G2D)
    }

    lateinit var grid: Grid<GraphicsElement>
    override fun setup() {
        frameRate(30f)

        val cellsX = 30
        val cellsY = 30

        grid = Grid(this, cellsX, cellsY)

        for (x in 0 until cellsX) {
            for (y in 0 until cellsY) {
                grid[x, y] = object : GraphicsElement(this) {
                    init {
                        width = (Math.random() * 20 + 10).toInt()
                        height = (Math.random() * 20 + 10).toInt()
                    }
                }
            }
        }

        graphicsManager.scale = 1

        grid.moveTo(30, 20)

        registerGraphicsElement(grid)
    }

    override fun draw() {
        background(0xCC)
    }
}

fun main() {
    PApplet.main(GridTest::class.java)
}
