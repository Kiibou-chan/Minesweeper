package space.kiibou.server

import space.kiibou.common.TileInfo

/** Test double that records every emitted event for assertions. */
class RecordingGameEvents : GameEvents {
    val reveals = mutableListOf<List<TileInfo>>()
    val flags = mutableListOf<Triple<Int, Int, Boolean>>()
    val bombsLeft = mutableListOf<Int>()
    val times = mutableListOf<Int>()
    var restarts = 0
    var wins = 0
    var loses = 0

    override fun revealTiles(tiles: List<TileInfo>) { reveals += tiles }
    override fun setFlag(x: Int, y: Int, flagged: Boolean) { flags += Triple(x, y, flagged) }
    override fun setBombsLeft(count: Int) { bombsLeft += count }
    override fun setTime(seconds: Int) { times += seconds }
    override fun restart() { restarts++ }
    override fun win() { wins++ }
    override fun lose() { loses++ }

    fun lastReveal(): List<TileInfo> = reveals.last()
}
