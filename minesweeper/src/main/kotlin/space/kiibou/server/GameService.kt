package space.kiibou.server

import space.kiibou.annotations.Inject
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.common.TilesInfo
import space.kiibou.net.common.ClientMessageType
import space.kiibou.net.common.Serial
import space.kiibou.net.common.ServerMessageType
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import space.kiibou.net.server.service.MessageService
import space.kiibou.net.server.service.RoutingService

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

    private val gameStates: HashMap<Long, GameState> = HashMap()

    override fun initialize() {
        routingService.registerCallback(MinesweeperMessageType.InitMap) {
            val gameState = getGameState(it.connectionHandle)
            val (width, height, bombs) = it.payload

            gameState.setupVariables(width, height, bombs)

            messageService.respond(
                it,
                MinesweeperMessageType.Restart
            )
        }

        routingService.registerCallback(MinesweeperMessageType.RevealTile) {
            val gameState = getGameState(it.connectionHandle)
            val (x, y) = it.payload

            val revealed = gameState.reveal(x, y)

            messageService.respond(
                it,
                MinesweeperMessageType.RevealTiles,
                TilesInfo(revealed)
            )
        }

        routingService.registerCallback(MinesweeperMessageType.ToggleFlag) {
            val gameState = getGameState(it.connectionHandle)
            val (x, y) = it.payload

            gameState.flagToggle(x, y)
        }

        routingService.registerCallback(MinesweeperMessageType.Restart) {
            val gameState = getGameState(it.connectionHandle)

            gameState.setupVariables()

            messageService.respond(
                it,
                MinesweeperMessageType.Restart
            )
        }

        server.onDisconnect {
            val gameState = getGameState(it)

            gameState.stopGame()

            removeGameState(it)
        }
    }

    private fun getGameState(handle: Long) =
        gameStates.computeIfAbsent(handle) { GameState(it, 9, 9, 10, this) }

    private fun removeGameState(handle: Long) =
        gameStates.remove(handle)
}
