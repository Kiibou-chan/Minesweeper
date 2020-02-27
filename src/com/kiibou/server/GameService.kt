package com.kiibou.server

import processing.data.JSONArray
import processing.data.JSONObject
import space.kiibou.data.Vec3
import space.kiibou.net.server.JSONMessage
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import space.kiibou.net.server.service.ActionService
import space.kiibou.reflect.Inject
import java.util.*

class GameService(server: Server) : Service(server) {
    @Inject
    lateinit var actionService: ActionService
    private val gameStates: HashMap<Long, GameState> = HashMap()

    override fun initialize() {
        actionService.addActionCallback("reveal-tiles") { message: JSONMessage -> revealTiles(message) }
        actionService.addActionCallback("restart") { message: JSONMessage -> restart(message) }
        actionService.addActionCallback("flag-toggle") { message: JSONMessage -> flagToggle(message) }
    }

    fun sendFlagToggle(handle: Long, x: Int, y: Int) {
        val gameState = getGameState(handle)
        actionService.sendActionToClient(handle, "toggle-flag", JSONObject()
                .setInt("x", x)
                .setInt("y", y)
                .setBoolean("toggle", gameState.flagToggle(x, y))
        )
    }

    private fun revealTiles(message: JSONMessage) {
        Objects.requireNonNull(message)
        val gameState = getGameState(message.connectionHandle)
        val x = message.message.getInt("x")
        val y = message.message.getInt("y")
        val revealed = gameState.reveal(x, y)
        sendRevealTiles(message.connectionHandle, revealed)
    }

    private fun restart(message: JSONMessage) {
        Objects.requireNonNull(message)
        val gameState = getGameState(message.connectionHandle)
        gameState.setupVariables()
        actionService.sendActionToClient(message.connectionHandle, "restart")
    }

    private fun flagToggle(message: JSONMessage) {
        Objects.requireNonNull(message)
        val x = message.message.getInt("x")
        val y = message.message.getInt("y")
        sendFlagToggle(message.connectionHandle, x, y)
    }

    private fun sendRevealTiles(handle: Long, revealed: List<Vec3>) {
        val message = JSONObject()
        val revealedTiles = JSONArray()
        for (pos in revealed) {
            val x = pos.x
            val y = pos.y
            val type = pos.z
            revealedTiles.append(JSONObject().setInt("x", x).setInt("y", y).setInt("type", type))
        }
        message.setJSONArray("revealed-tiles", revealedTiles)
        actionService.sendActionToClient(handle, "reveal-tiles", message)
    }

    fun sendWin(handle: Long) = actionService.sendActionToClient(handle, "win")

    fun sendLoose(handle: Long) = actionService.sendActionToClient(handle, "loose")

    fun sendTime(handle: Long, time: Int) =
            actionService.sendActionToClient(handle, "set-time", JSONObject().setInt("time", time))

    private fun getGameState(handle: Long) =
            gameStates.computeIfAbsent(handle) { GameState(it, 18, 18, 50, this) }

}