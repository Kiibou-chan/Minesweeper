package space.kiibou.server

import space.kiibou.common.TileInfo

/**
 * Output port for everything a running game emits. [GameState] talks to this
 * interface instead of the network directly, which lets tests observe game
 * behaviour without any transport. The production implementation broadcasts to
 * every connection in the game; see [BroadcastGameEvents].
 */
interface GameEvents {
    fun revealTiles(tiles: List<TileInfo>)
    fun setFlag(x: Int, y: Int, flagged: Boolean)
    fun setBombsLeft(count: Int)
    fun setTime(seconds: Int)
    fun restart()
    fun win()
    fun lose()
}
