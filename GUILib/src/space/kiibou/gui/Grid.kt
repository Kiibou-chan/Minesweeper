package space.kiibou.gui

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableNumberValue
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import space.kiibou.GApplet

class Grid<T : GraphicsElement>(app: GApplet, x: Int, y: Int, private val cellsX: Int, private val cellsY: Int, scale: Int)
    : GraphicsElement(app, x, y, 0, 0, scale), Iterable<T> {

    private val colWidths = Array(cellsX) { SimpleIntegerProperty(0) to SimpleIntegerProperty(0) }.also {
        it.forEachIndexed { index, pair ->
            if (index != 0) pair.second.bind(it[index - 1].second.add(pair.first))
        }
    }
    private val rowHeights = Array(cellsY) { SimpleIntegerProperty(0) to SimpleIntegerProperty(0) }.also {
        it.forEachIndexed { index, pair ->
            if (index != 0) pair.second.bind(it[index - 1].first.add(it[index - 1].second))
        }
    }

    private val cells = FXCollections.observableHashMap<Pair<Int, Int>, T>().also {
        it.addListener { change: MapChangeListener.Change<out Pair<Int, Int>, out T>? ->
            val (xi, yi) = change!!.key
            var w: ObservableNumberValue = SimpleIntegerProperty(0)
            var h: ObservableNumberValue = SimpleIntegerProperty(0)
            for (yIndex in 0 until cellsY) {
                val cell = get(xi, yIndex)
                if (cell != null)
                    w = Bindings.max(w, cell.widthProp)
            }
            for (xIndex in 0 until cellsX) {
                val cell = get(xIndex, yi)
                if (cell != null)
                    h = Bindings.max(h, cell.heightProp)
            }
            colWidths[yi].first.bind(w)
            rowHeights[xi].first.bind(h)
        }
    }

    init {
        widthProp.bind(colWidths.last().let { it.first.add(it.second) })
        heightProp.bind(rowHeights.last().let { it.first.add(it.second) })
    }

    override fun preInitImpl() {}
    public override fun initImpl() {}
    override fun postInitImpl() {}
    override fun drawImpl() {}

    operator fun set(x: Int, y: Int, element: T) {
        if (isValidCell(x, y)) {
            remove(x, y)
            cells[x, y] = element
            addChild(element)
            element.xProp.bind(colWidths[x].second.add(xProp))
            element.yProp.bind(rowHeights[y].second.add(yProp))
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
                removeChild(index).also {
                    it.xProp.unbind()
                    it.yProp.unbind()
                }
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
