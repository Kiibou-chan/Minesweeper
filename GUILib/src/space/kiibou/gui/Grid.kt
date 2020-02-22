package space.kiibou.gui

import space.kiibou.GApplet
import java.util.*

class Grid<T : GraphicsElement>(app: GApplet, x: Int, y: Int, private val cellsX: Int, private val cellsY: Int, scale: Int)
    : GraphicsElement(app, x, y, 0, 0, scale), Iterable<T> {

    private val cells = HashMap<Pair<Int, Int>, T>()

    override fun preInitImpl() {}
    public override fun initImpl() {
        val colWidth = IntArray(cellsX)
        val rowHeight = IntArray(cellsY)

        for (x in 0 until cellsX) {
            for (y in 0 until cellsY) {
                cells[x, y]?.let {
                    colWidth[x] = colWidth[x].coerceAtLeast(it.width)
                    rowHeight[y] = rowHeight[y].coerceAtLeast(it.height)
                }
            }
        }

        var width = 0
        var height = 0
        for (cx in 0 until cellsX) {
            height = 0
            for (cy in 0 until cellsY) {
                cells[cx to cy]?.moveTo(x + width, y + height)
                height += rowHeight[cy]
            }
            width += colWidth[cx]
        }

        this.width = width
        this.height = height
    }

    override fun postInitImpl() {}

    override fun drawImpl() {}

    operator fun set(x: Int, y: Int, element: T) {
        if (isValidCell(x, y)) {
            val e = cells[x, y]
            if (e != null) {
                val index = children.indexOf(e)
                removeChild(index)
            }

            this.cells[x, y] = element
            addChild(element)
        } else {
            throw IndexOutOfBoundsException(String.format("passed x:%d, y:%d must be in range x:0-%d, y:0-%d", x, y, cellsX, cellsY))
        }
    }

    operator fun get(x: Int, y: Int): T? {
        return if (isValidCell(x, y)) {
            this.cells[x, y]
        } else {
            throw IndexOutOfBoundsException(String.format("passed x:%d, y:%d must be in range x:0-%d, y:0-%d", x, y, cellsX, cellsY))
        }
    }

    fun remove(x: Int, y: Int): T? {
        return if (isValidCell(x, y)) {
            if (this.cells[x, y] != null) {
                val index = children.indexOf(this.cells[x, y]!!)
                removeChild(index)
                cells.remove(x, y)
            } else {
                null
            }
        } else {
            throw IndexOutOfBoundsException(String.format("passed x:%d, y:%d must be in range x:0-%d, y:0-%d", x, y, cellsX, cellsY))
        }
    }

    private fun isValidCell(x: Int, y: Int): Boolean {
        return x in 0 until cellsX && y in 0 until cellsY
    }

    override fun iterator(): MutableIterator<T> {
        return cells.values.iterator()
    }

}

private operator fun <A, B, C> Map<Pair<A, B>, C>.get(key: A, value: B) = this[key to value]
private operator fun <A, B, C> MutableMap<Pair<A, B>, C>.set(key1: A, key2: B, value: C) = set(key1 to key2, value)
private fun <A, B, C> MutableMap<Pair<A, B>, C>.remove(key: A, value: B) = this.remove(key to value)
