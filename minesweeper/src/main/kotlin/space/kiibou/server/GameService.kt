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

    private val registry = RoomRegistry(::newRoom)

    private fun newRoom(roomHandle: GameHandle): Room {
        lateinit var room: Room

        room = Room(roomHandle, BroadcastRoomEvents({ room.members }, messageService)) { settings, handles ->
            GameState(
                handles, settings.width, settings.height, settings.bombs,
                GameOverNotifying(BroadcastGameEvents(handles, messageService)) { room.onGameOver() },
                FixedRateTicker("Timer $roomHandle"),
            )
        }

        return room
    }

    override fun initialize() {
        routingService.registerCallback(MinesweeperMessageType.CreateRoom) {
            registry.createAndJoin(it.connectionHandle)
        }

        routingService.registerCallback(MinesweeperMessageType.ListRooms) {
            messageService.send(
                it.connectionHandle,
                MinesweeperMessageType.RoomList,
                RoomListInfo(registry.listLobbyRooms()),
            )
        }

        routingService.registerCallback(MinesweeperMessageType.JoinRoom) { message ->
            val joined = registry.join(message.connectionHandle, message.payload)
            // A playing room refuses via its own RoomEvents; only an unknown room needs a
            // refusal from here.
            if (!joined && registry.room(message.payload) == null) {
                messageService.send(message.connectionHandle, MinesweeperMessageType.JoinRefused, message.payload)
            }
        }

        routingService.registerCallback(MinesweeperMessageType.LeaveRoom) {
            registry.leave(it.connectionHandle)
        }

        routingService.registerCallback(MinesweeperMessageType.SetReady) {
            withRoom(it.connectionHandle) { setReady(it.connectionHandle, it.payload.ready) }
        }

        routingService.registerCallback(MinesweeperMessageType.SetSettings) {
            withRoom(it.connectionHandle) { setSettings(it.connectionHandle, it.payload) }
        }

        routingService.registerCallback(MinesweeperMessageType.StartGame) {
            withRoom(it.connectionHandle) { startGame(it.connectionHandle) }
        }

        routingService.registerCallback(MinesweeperMessageType.RevealTile) {
            val (x, y) = it.payload
            withGame(it.connectionHandle) { revealAt(x, y) }
        }

        routingService.registerCallback(MinesweeperMessageType.ToggleFlag) {
            val (x, y) = it.payload
            withGame(it.connectionHandle) { flagToggle(x, y) }
        }

        // Solo-compat shim for the pre-lobby client: JoinGame joins-or-creates the room and
        // auto-readies; InitMap applies settings and immediately starts a single-member room.
        // Delete both once the client speaks the room protocol (SP-3 client stage).
        routingService.registerCallback(MinesweeperMessageType.JoinGame) { message ->
            if (!registry.join(message.connectionHandle, message.payload)) {
                val room = registry.createAndJoin(message.connectionHandle)
                logger.info { "Compat: created room ${room.handle} for JoinGame(${message.payload})" }
            }
            withRoom(message.connectionHandle) { setReady(message.connectionHandle, true) }
        }

        routingService.registerCallback(MinesweeperMessageType.InitMap) { message ->
            withRoom(message.connectionHandle) {
                setSettings(message.connectionHandle, message.payload)
                if (members.size == 1) startGame(message.connectionHandle)
            }
        }

        routingService.registerCallback(MinesweeperMessageType.Restart) { message ->
            // Compat: the smiley restart maps to game-over-and-restart for single-member rooms.
            withRoom(message.connectionHandle) {
                onGameOver()
                if (members.size == 1) {
                    setReady(message.connectionHandle, true)
                    startGame(message.connectionHandle)
                }
            }
        }

        server.onDisconnect { registry.leave(it) }
    }

    private fun withRoom(handle: ConnectionHandle, action: Room.() -> Unit) {
        val room = registry.roomFor(handle)
        if (room == null) {
            logger.warn { "Room message from connection $handle outside any room; ignoring" }
            return
        }
        room.action()
    }

    private fun withGame(handle: ConnectionHandle, action: GameState.() -> Unit) {
        val game = registry.roomFor(handle)?.gameState
        if (game == null) {
            logger.warn { "Game message from connection $handle with no running game; ignoring" }
            return
        }
        game.action()
    }
}

/** Production [RoomEvents]: broadcasts room updates to the room's current members. */
class BroadcastRoomEvents(
    private val members: () -> List<ConnectionHandle>,
    private val messageService: MessageService,
) : RoomEvents {
    override fun roomState(state: RoomStateInfo) =
        members().forEach { messageService.send(it, MinesweeperMessageType.RoomState, state) }

    override fun joinRefused(to: ConnectionHandle, room: GameHandle) =
        messageService.send(to, MinesweeperMessageType.JoinRefused, room)

    override fun gameStarted() =
        members().forEach { messageService.send(it, MinesweeperMessageType.GameStarted) }
}

/** Decorates [GameEvents] to flip the room back to its lobby when the game ends. */
class GameOverNotifying(
    private val inner: GameEvents,
    private val onGameOver: () -> Unit,
) : GameEvents by inner {
    override fun win() {
        inner.win()
        onGameOver()
    }

    override fun lose() {
        inner.lose()
        onGameOver()
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
