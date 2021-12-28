package space.kiibou.server

import space.kiibou.common.MinesweeperAction
import space.kiibou.common.TilesInfo
import space.kiibou.net.common.Action
import space.kiibou.net.reflect.Inject
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import space.kiibou.net.server.service.ActionService

class GameService(server: Server) : Service(server) {
    @Inject
    lateinit var actionService: ActionService

    private val gameStates: HashMap<Long, GameState> = HashMap()

    override fun initialize() {
        MinesweeperAction

        actionService.registerCallback<MinesweeperAction.InitMap> { (handle, content) ->
            val gameState = getGameState(handle)

            gameState.setupVariables(content.data.width, content.data.height, content.data.bombs)

            send(handle, MinesweeperAction.Restart)
        }

        actionService.registerCallback<MinesweeperAction.RevealTile> { (handle, content) ->
            val gameState = getGameState(handle)

            val revealed = gameState.reveal(content.data.x, content.data.y)

            send(handle, MinesweeperAction.RevealTiles(TilesInfo(revealed)))
        }

        actionService.registerCallback<MinesweeperAction.ToggleFlag> { (handle, content) ->
            val gameState = getGameState(handle)

            gameState.flagToggle(content.data.x, content.data.y)
        }

        actionService.registerCallback<MinesweeperAction.Restart> { (handle, _) ->
            val gameState = getGameState(handle)

            gameState.setupVariables()

            send(handle, MinesweeperAction.Restart)
        }
    }

    fun <S, T : Action<S>> send(handle: Long, action: T) {
        actionService.send(handle, action)
    }

    private fun getGameState(handle: Long) =
        gameStates.computeIfAbsent(handle) { GameState(it, 9, 9, 10, this) }
}
