package space.kiibou.server

import space.kiibou.annotations.AutoLoad
import space.kiibou.annotations.Inject
import space.kiibou.common.GameHandle
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.common.TilesInfo
import space.kiibou.net.common.ClientMessageType
import space.kiibou.net.common.ConnectionHandle
import space.kiibou.net.common.Serial
import space.kiibou.net.common.ServerMessageType
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import space.kiibou.net.server.service.MessageService
import space.kiibou.net.server.service.RoutingService

@AutoLoad
class GameService(server: Server) : Service(server) {

    companion object {
        init {
            Serial.addModule(ClientMessageType.serializersModule)
            Serial.addModule(ServerMessageType.serializersModule)
            Serial.addModule(MinesweeperMessageType.serializersModule)
        }
    }

    @Inject
    lateinit var routingService: RoutingService

    @Inject
    lateinit var messageService: MessageService

    private val users: MutableMap<ConnectionHandle, GameHandle> = mutableMapOf()

    private val gameStates: MutableMap<GameHandle, GameState> = mutableMapOf()

    override fun initialize() {
        routingService.registerCallback(MinesweeperMessageType.JoinGame) {
            joinGame(it.connectionHandle, it.payload)
        }

        routingService.registerCallback(MinesweeperMessageType.InitMap) {
            val gameState = getGameState(it.connectionHandle)
            val (width, height, bombs) = it.payload

            gameState.setupVariables(width, height, bombs)

            gameState.handles.forEach { handle ->
                messageService.send(handle, MinesweeperMessageType.Restart)
            }
        }

        routingService.registerCallback(MinesweeperMessageType.RevealTile) {
            val gameState = getGameState(it.connectionHandle)
            val (x, y) = it.payload

            val revealed = gameState.reveal(x, y)

            gameState.handles.forEach { handle ->
                messageService.send(
                    handle,
                    MinesweeperMessageType.RevealTiles,
                    TilesInfo(revealed)
                )
            }
        }

        routingService.registerCallback(MinesweeperMessageType.ToggleFlag) {
            val gameState = getGameState(it.connectionHandle)
            val (x, y) = it.payload

            gameState.flagToggle(x, y)
        }

        routingService.registerCallback(MinesweeperMessageType.Restart) {
            val gameState = getGameState(it.connectionHandle)

            gameState.setupVariables()

            gameState.handles.forEach { handle ->
                messageService.send(handle, MinesweeperMessageType.Restart)
            }
        }

        server.onDisconnect {
            val gameState = getGameState(it)
            gameState.removePlayer(it)
            gameState.stopGame()
        }
    }

    private fun getGameState(handle: ConnectionHandle): GameState {
        val gameHandle = users[handle]!!
        return gameStates[gameHandle]!!
    }

    private fun joinGame(handle: ConnectionHandle, gameHandle: GameHandle) {
        users[handle] = gameHandle
        gameStates.computeIfAbsent(gameHandle) { GameState(mutableListOf(), 9, 9, 10, this) }
        gameStates[gameHandle]!!.addPlayer(handle)
    }

}
