package com.kiibou.server

import com.kiibou.common.*
import space.kiibou.net.common.Message
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

        actionService.registerCallback(this::revealTile)
        actionService.registerCallback(this::restart)
        actionService.registerCallback(this::flagToggle)
        actionService.registerCallback(this::initMap)
    }

    private fun initMap(message: Message<MinesweeperAction.InitMap>) {
        val gameState = getGameState(message.connectionHandle)
        val (width, height, bombs) = message.content.data
        gameState.setupVariables(width, height, bombs)
        actionService.send(message.connectionHandle, MinesweeperAction.Restart)
    }

    private fun revealTile(message: Message<MinesweeperAction.RevealTile>) {
        val gameState = getGameState(message.connectionHandle)
        val (x, y) = message.content.data
        val revealed = gameState.reveal(x, y)
        sendRevealTiles(message.connectionHandle, revealed)
    }

    private fun restart(message: Message<MinesweeperAction.Restart>) {
        val gameState = getGameState(message.connectionHandle)
        gameState.setupVariables()
        actionService.send(message.connectionHandle, MinesweeperAction.Restart)
    }

    private fun flagToggle(message: Message<MinesweeperAction.ToggleFlag>) {
        val gameState = getGameState(message.connectionHandle)

        // TODO (Svenja, 20/12/2021): Change client action to ToggleFlag(x, y) and server action to SetFlag(x, y, status)
        val (x, y, state) = message.content.data
        gameState.flagToggle(x, y)
        sendFlagStatus(message.connectionHandle, x, y)
    }

    fun sendFlagStatus(handle: Long, x: Int, y: Int) {
        val gameState = getGameState(handle)
        actionService.send(handle, MinesweeperAction.ToggleFlag(FlagInfo(x, y, gameState.isFlagged(x, y))))
    }

    private fun sendRevealTiles(handle: Long, revealed: List<TileInfo>) {
        actionService.send(handle, MinesweeperAction.RevealTiles(TilesInfo(revealed)))
    }

    fun sendWin(handle: Long) = actionService.send(handle, MinesweeperAction.Win)

    fun sendLoose(handle: Long) = actionService.send(handle, MinesweeperAction.Loose)

    fun sendTime(handle: Long, time: Int) =
        actionService.send(handle, MinesweeperAction.SetTime(TimeInfo(time)))

    private fun getGameState(handle: Long) =
        gameStates.computeIfAbsent(handle) { GameState(it, 9, 9, 10, this) }
}
