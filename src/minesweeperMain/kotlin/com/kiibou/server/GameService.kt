package com.kiibou.server

import com.fasterxml.jackson.databind.JsonNode
import com.kiibou.*
import space.kiibou.data.Vec2
import space.kiibou.net.common.Message
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
        actionService.registerCallback("reveal-tiles", ::revealTiles)
        actionService.registerCallback("restart", ::restart)
        actionService.registerCallback("flag-toggle", ::flagToggle)
        actionService.registerCallback("init-map", ::initMap)
    }

    private fun initMap(message: Message<JsonNode>) {
        val gameState = getGameState(message.connectionHandle)
        val (width, height, bombs) = json.mapper.treeToValue(message.content, MapInfo::class.java)
        gameState.setupVariables(width, height, bombs)
        actionService.send(message.connectionHandle, "restart")
    }

    private fun revealTiles(message: Message<JsonNode>) {
        val gameState = getGameState(message.connectionHandle)
        val (x, y) = json.mapper.treeToValue(message.content, Vec2::class.java)
        val revealed = gameState.reveal(x, y)
        sendRevealTiles(message.connectionHandle, revealed)
    }

    private fun restart(message: Message<JsonNode>) {
        val gameState = getGameState(message.connectionHandle)
        gameState.setupVariables()
        actionService.send(message.connectionHandle, "restart")
    }

    private fun flagToggle(message: Message<JsonNode>) {
        val gameState = getGameState(message.connectionHandle)
        val (x, y) = json.mapper.treeToValue(message.content, Vec2::class.java)
        gameState.flagToggle(x, y)
        sendFlagStatus(message.connectionHandle, x, y)
    }

    fun sendFlagStatus(handle: Long, x: Int, y: Int) {
        val gameState = getGameState(handle)
        actionService.send(handle, "toggle-flag", json.mapper.valueToTree(FlagInfo(x, y, gameState.isFlagged(x, y))))
    }

    private fun sendRevealTiles(handle: Long, revealed: List<TileInfo>) {
        actionService.send(handle, "reveal-tiles", json.mapper.valueToTree(RevealTiles(revealed)))
    }

    fun sendWin(handle: Long) = actionService.send(handle, "win")

    fun sendLoose(handle: Long) = actionService.send(handle, "loose")

    fun sendTime(handle: Long, time: Int) =
        actionService.send(handle, "set-time", json.mapper.valueToTree(TimeInfo(time)))

    private fun getGameState(handle: Long) =
            gameStates.computeIfAbsent(handle) { GameState(it, 9, 9, 10, this) }

}