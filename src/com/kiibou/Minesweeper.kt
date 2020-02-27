package com.kiibou

import com.kiibou.server.GameService
import processing.core.PApplet
import processing.data.JSONObject
import processing.opengl.PGraphicsOpenGL
import processing.opengl.PJOGL
import space.kiibou.GApplet
import space.kiibou.gui.GGraphics
import space.kiibou.net.NetUtils
import space.kiibou.net.client.Client
import space.kiibou.net.common.ActionDispatcher
import space.kiibou.net.server.startServer

class Minesweeper : GApplet() {
    private lateinit var map: Map
    lateinit var client: Client
    private lateinit var dispatcher: ActionDispatcher<JSONObject>

    override fun settings() {
        size(800, 800, GGraphics::class.java.canonicalName)
        //        fullScreen(P2D)
        PJOGL.setIcon("pictures/icon.png")
    }

    override fun setup() {
        surface.setTitle("Minesweeper")
        (g as PGraphicsOpenGL).textureSampling(2)
        frameRate(60f)
        map = Map(this, 0, 0, 18, 18, 2, 50)
        registerGraphicsElement(map)

        dispatcher = ActionDispatcher {
            if (it.hasKey("action")) {
                val action = it.getString("action")
                dispatcher.dispatchAction(action, it)
            }
        }

        dispatcher.addActionCallback("set-time", ::setTime)
        dispatcher.addActionCallback("reveal-tiles", ::revealTiles)
        dispatcher.addActionCallback("win") { map.win() }
        dispatcher.addActionCallback("loose") { map.loose() }
        dispatcher.addActionCallback("restart") { map.restart() }
        dispatcher.addActionCallback("toggle-flag", ::toggleFlag)

        client = Client(
                ::onServerConnect,
                dispatcher.messageReceived,
                ::onServerDisconnect
        ).connect("localhost", 8454)
    }

    private fun onServerConnect() {
        println("Connected to Server!")
    }

    private fun onServerDisconnect() {
        println("Disconnected from Server!")
        exit()
    }

    override fun draw() {
        if (width != map.width || height != map.height) {
            surface.setSize(map.width, map.height)
        }

        background(204)
    }

    private fun revealTiles(o: JSONObject) {
        val revealedTiles = o.getJSONArray("revealed-tiles")
        for (i in 0 until revealedTiles.size()) {
            val data = revealedTiles.getJSONObject(i)
            val x = data.getInt("x")
            val y = data.getInt("y")
            val type = data.getInt("type")
            map.revealTile(x, y, TileType.getTypeFromValue(type))
        }
    }

    private fun setTime(o: JSONObject) {
        map.controlBar.timerDisplay.value = o.getInt("time")
    }

    private fun toggleFlag(o: JSONObject) {
        val x = o.getInt("x")
        val y = o.getInt("y")
        val flag = o.getBoolean("toggle")
        map.tileFlag(x, y, flag)
    }
}

fun main() {
    if (!NetUtils.checkServerListening("localhost", 8454, 200)) {
        /*
         */
        startServer(GameService::class.java).ifPresent { server: Process ->
            println("Starting Server")
            Runtime.getRuntime().addShutdownHook(
                    Thread {
                        server.destroy()
                        println("Stopped Server")
                    }
            )
        }

        // serverMain(arrayOf("--port=8454", "--services=" + GameService::class.java.canonicalName))
    }

    PApplet.main(Minesweeper::class.java)
}
