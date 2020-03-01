package com.kiibou.server

import com.kiibou.FlagInfo
import com.kiibou.RevealTiles
import com.kiibou.TileInfo
import com.kiibou.TimeInfo
import space.kiibou.data.Vec2
import space.kiibou.net.server.JsonMessage
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import space.kiibou.net.server.service.ActionService
import space.kiibou.net.server.service.JsonService
import space.kiibou.reflect.Inject
import java.util.*

class GameService(server: Server) : Service(server) {
    @Inject
    lateinit var actionService: ActionService
    @Inject
    lateinit var json: JsonService
    private val gameStates: HashMap<Long, GameState> = HashMap()

    override fun initialize() {
        actionService.addActionCallback("reveal-tiles") { message: JsonMessage -> revealTiles(message) }
        actionService.addActionCallback("restart") { message: JsonMessage -> restart(message) }
        actionService.addActionCallback("flag-toggle") { message: JsonMessage -> flagToggle(message) }
    }

    private fun revealTiles(message: JsonMessage) {
        val gameState = getGameState(message.connectionHandle)
        val (x, y) = json.mapper.treeToValue(message.node, Vec2::class.java)
        val revealed = gameState.reveal(x, y)
        sendRevealTiles(message.connectionHandle, revealed)
    }

    private fun restart(message: JsonMessage) {
        val gameState = getGameState(message.connectionHandle)
        gameState.setupVariables()
        actionService.sendActionToClient(message.connectionHandle, "restart")
    }

    private fun flagToggle(message: JsonMessage) {
        val gameState = getGameState(message.connectionHandle)
        val (x, y) = json.mapper.treeToValue(message.node, Vec2::class.java)
        gameState.flagToggle(x, y)
        sendFlagToggle(message.connectionHandle, x, y)
    }

    fun sendFlagToggle(handle: Long, x: Int, y: Int) {
        val gameState = getGameState(handle)
        actionService.sendActionToClient(handle, "toggle-flag", json.mapper.valueToTree(FlagInfo(x, y, gameState.flag(x, y))))
    }

    private fun sendRevealTiles(handle: Long, revealed: List<TileInfo>) {
        actionService.sendActionToClient(handle, "reveal-tiles", json.mapper.valueToTree(RevealTiles(revealed)))
    }

    fun sendWin(handle: Long) = actionService.sendActionToClient(handle, "win")

    fun sendLoose(handle: Long) = actionService.sendActionToClient(handle, "loose")

    fun sendTime(handle: Long, time: Int) =
            actionService.sendActionToClient(handle, "set-time", json.mapper.valueToTree(TimeInfo(time)))

    private fun getGameState(handle: Long) =
            gameStates.computeIfAbsent(handle) { GameState(it, 18, 18, 50, this) }

}