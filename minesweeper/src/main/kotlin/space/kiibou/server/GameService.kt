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

    /**
     * Message callbacks run on per-connection socket listener threads; every room/game
     * mutation is serialized on this lock to keep the state maps consistent.
     */
    private val stateLock = Any()

    private val names = mutableMapOf<ConnectionHandle, String>()

    private val registry = RoomRegistry(::newRoom)

    private fun nameOf(handle: ConnectionHandle): String =
        names[handle] ?: "Player ${handle.handle}"

    private fun newRoom(roomHandle: GameHandle): Room {
        lateinit var room: Room

        room = Room(roomHandle, BroadcastRoomEvents({ room.members }, messageService), ::nameOf) { settings, handles ->
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
            synchronized(stateLock) { registry.createAndJoin(it.connectionHandle) }
        }

        routingService.registerCallback(MinesweeperMessageType.ListRooms) {
            val rooms = synchronized(stateLock) { registry.listLobbyRooms() }
            messageService.send(it.connectionHandle, MinesweeperMessageType.RoomList, RoomListInfo(rooms))
        }

        routingService.registerCallback(MinesweeperMessageType.JoinRoom) { message ->
            val refuse = synchronized(stateLock) {
                val joined = registry.join(message.connectionHandle, message.payload)
                // A playing room refuses via its own RoomEvents; only an unknown room
                // needs a refusal from here.
                !joined && registry.room(message.payload) == null
            }
            if (refuse) {
                messageService.send(message.connectionHandle, MinesweeperMessageType.JoinRefused, message.payload)
            }
        }

        routingService.registerCallback(MinesweeperMessageType.LeaveRoom) {
            synchronized(stateLock) { registry.leave(it.connectionHandle) }
        }

        routingService.registerCallback(MinesweeperMessageType.SetReady) {
            synchronized(stateLock) { withRoom(it.connectionHandle) { setReady(it.connectionHandle, it.payload.ready) } }
        }

        routingService.registerCallback(MinesweeperMessageType.SetSettings) {
            synchronized(stateLock) { withRoom(it.connectionHandle) { setSettings(it.connectionHandle, it.payload) } }
        }

        routingService.registerCallback(MinesweeperMessageType.SetName) {
            synchronized(stateLock) {
                names[it.connectionHandle] = it.payload.name.take(24)
                registry.roomFor(it.connectionHandle)?.refreshState()
            }
        }

        routingService.registerCallback(MinesweeperMessageType.StartGame) {
            synchronized(stateLock) { withRoom(it.connectionHandle) { startGame(it.connectionHandle) } }
        }

        routingService.registerCallback(MinesweeperMessageType.RevealTile) {
            val (x, y) = it.payload
            synchronized(stateLock) { withGame(it.connectionHandle) { revealAt(x, y) } }
        }

        routingService.registerCallback(MinesweeperMessageType.ToggleFlag) {
            val (x, y) = it.payload
            synchronized(stateLock) { withGame(it.connectionHandle) { flagToggle(x, y) } }
        }

        server.onDisconnect {
            synchronized(stateLock) {
                registry.leave(it)
                names.remove(it)
            }
        }
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

    override fun memberId(to: ConnectionHandle, id: Long) =
        messageService.send(to, MinesweeperMessageType.YourId, YourIdInfo(id))

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
