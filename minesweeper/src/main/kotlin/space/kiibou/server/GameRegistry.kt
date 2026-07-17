package space.kiibou.server

import space.kiibou.common.GameHandle
import space.kiibou.net.common.ConnectionHandle

/**
 * Maps each connection to the game it joined and owns game lifecycle: a game is created on the
 * first join and removed once its last player leaves. Every lookup tolerates an unknown
 * connection, so a game message or a disconnect that arrives before [join] is a safe no-op
 * instead of a crash.
 */
class GameRegistry(private val createGame: (GameHandle) -> GameState) {

    private val users: MutableMap<ConnectionHandle, GameHandle> = mutableMapOf()
    private val games: MutableMap<GameHandle, GameState> = mutableMapOf()

    fun join(handle: ConnectionHandle, game: GameHandle): GameState {
        users[handle] = game
        return games.getOrPut(game) { createGame(game) }.also { it.addPlayer(handle) }
    }

    fun gameFor(handle: ConnectionHandle): GameState? = users[handle]?.let { games[it] }

    fun leave(handle: ConnectionHandle) {
        val game = users.remove(handle) ?: return
        val state = games[game] ?: return
        state.removePlayer(handle)
        if (state.handles.isEmpty()) games.remove(game)
    }

    fun activeGameCount(): Int = games.size
}
