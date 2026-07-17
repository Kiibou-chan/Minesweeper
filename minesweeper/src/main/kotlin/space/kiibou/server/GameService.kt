package space.kiibou.server

import space.kiibou.annotations.AutoLoad
import space.kiibou.annotations.Inject
import space.kiibou.common.*
import space.kiibou.net.common.ClientMessageType
import space.kiibou.net.common.ConnectionHandle
import space.kiibou.net.common.MessageType
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

            gameState.reset(width, height, bombs)
        }

        routingService.registerCallback(MinesweeperMessageType.RevealTile) {
            val gameState = getGameState(it.connectionHandle)
            val (x, y) = it.payload

            gameState.revealAt(x, y)
        }

        routingService.registerCallback(MinesweeperMessageType.ToggleFlag) {
            val gameState = getGameState(it.connectionHandle)
            val (x, y) = it.payload

            gameState.flagToggle(x, y)
        }

        routingService.registerCallback(MinesweeperMessageType.Restart) {
            val gameState = getGameState(it.connectionHandle)

            gameState.reset()
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
        gameStates.computeIfAbsent(gameHandle) { gh ->
            val handles = mutableListOf<ConnectionHandle>()
            GameState(
                handles, 9, 9, 10,
                BroadcastGameEvents(handles, messageService),
                FixedRateTicker("Timer $gh"),
            )
        }
        gameStates[gameHandle]!!.addPlayer(handle)
    }
}

/** Production [GameEvents] that broadcasts each event to every connection in the game. */
class BroadcastGameEvents(
    private val handles: List<ConnectionHandle>,
    private val messageService: MessageService,
) : GameEvents {
    override fun revealTiles(tiles: List<TileInfo>) =
        broadcast(MinesweeperMessageType.RevealTiles, TilesInfo(tiles))

    override fun setFlag(x: Int, y: Int, flagged: Boolean) =
        broadcast(MinesweeperMessageType.SetFlag, FlagInfo(x, y, flagged))

    override fun setBombsLeft(count: Int) =
        broadcast(MinesweeperMessageType.SetBombsLeft, BombsLeftInfo(count))

    override fun setTime(seconds: Int) =
        broadcast(MinesweeperMessageType.SetTime, TimeInfo(seconds))

    override fun restart() = broadcast(MinesweeperMessageType.Restart)

    override fun win() = broadcast(MinesweeperMessageType.Win)

    override fun lose() = broadcast(MinesweeperMessageType.Loose)

    private fun <T : Any> broadcast(type: MessageType<T>, payload: T) =
        handles.forEach { messageService.send(it, type, payload) }

    private fun broadcast(type: MessageType<Unit>) =
        handles.forEach { messageService.send(it, type) }
}
