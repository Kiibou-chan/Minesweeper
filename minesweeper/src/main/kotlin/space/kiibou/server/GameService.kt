package space.kiibou.server

import mu.KotlinLogging
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

private val logger = KotlinLogging.logger { }

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

    private val registry = GameRegistry { gameHandle ->
        val handles = mutableListOf<ConnectionHandle>()
        GameState(
            handles, 9, 9, 10,
            BroadcastGameEvents(handles, messageService),
            FixedRateTicker("Timer $gameHandle"),
        )
    }

    override fun initialize() {
        routingService.registerCallback(MinesweeperMessageType.JoinGame) {
            registry.join(it.connectionHandle, it.payload)
        }

        routingService.registerCallback(MinesweeperMessageType.InitMap) {
            val (width, height, bombs) = it.payload
            withGame(it.connectionHandle) { reset(width, height, bombs) }
        }

        routingService.registerCallback(MinesweeperMessageType.RevealTile) {
            val (x, y) = it.payload
            withGame(it.connectionHandle) { revealAt(x, y) }
        }

        routingService.registerCallback(MinesweeperMessageType.ToggleFlag) {
            val (x, y) = it.payload
            withGame(it.connectionHandle) { flagToggle(x, y) }
        }

        routingService.registerCallback(MinesweeperMessageType.Restart) {
            withGame(it.connectionHandle) { reset() }
        }

        server.onDisconnect { registry.leave(it) }
    }

    /** Runs [action] on the caller's game, or logs and ignores when the connection has not joined one. */
    private fun withGame(handle: ConnectionHandle, action: GameState.() -> Unit) {
        val game = registry.gameFor(handle)
        if (game == null) {
            logger.warn { "Game message from connection $handle with no joined game; ignoring" }
            return
        }
        game.action()
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
