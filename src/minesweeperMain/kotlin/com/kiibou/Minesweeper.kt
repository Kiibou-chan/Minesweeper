@file:JvmName("MinesweeperMain")

package com.kiibou

import com.kiibou.common.MinesweeperAction
import com.kiibou.server.GameService
import processing.core.PApplet
import processing.opengl.PGraphicsOpenGL
import processing.opengl.PJOGL
import space.kiibou.GApplet
import space.kiibou.gui.GGraphics
import space.kiibou.net.NetUtils
import space.kiibou.net.client.ActionClient
import space.kiibou.net.client.Client
import space.kiibou.net.common.Action
import space.kiibou.net.server.main

class Minesweeper : GApplet() {
    private lateinit var map: Map
    lateinit var client: Client<Action<*>>

    override fun settings() {
        size(800, 800, GGraphics::class.java.canonicalName)
        setScale(2)
        //        fullScreen(P2D)
        PJOGL.setIcon("pictures/icon_30.png")

        MinesweeperAction
    }

    override fun setup() {
        surface.setTitle("Minesweeper")
        surface.setResizable(true)
        (g as PGraphicsOpenGL).textureSampling(2)
        frameRate(60f)
        map = Map(this, 9, 9, 10)
        registerGraphicsElement(map)

        registerActionCallback(map.controlBar::setTime)
        registerActionCallback(map::revealTiles)
        registerActionCallback(map::win)
        registerActionCallback(map::loose)
        registerActionCallback(map::restart)
        registerActionCallback(map::setFlag)
        registerActionCallback(map.controlBar::setBombsLeft)

        client = ActionClient(
            ::onServerConnect,
            eventDispatcher::actionEvent,
            ::onServerDisconnect
        ).connect("localhost", 8454)
    }

    private fun onServerConnect() {
        println("[Client] Connected to Server!")
    }

    private fun onServerDisconnect() {
        println("[Client] Disconnected from Server!")
        exit()
    }

    override fun draw() {
        if (width < map.width) surface.setSize(map.width, height)
        if (height < map.height) surface.setSize(width, map.height)

        val cX = width / 2 - map.width / 2
        if (map.x != cX) map.moveTo(cX, map.y)

        val cY = height / 2 - map.height / 2
        if (map.y != cY) map.moveTo(map.x, cY)

        background(0xFF)
    }


    private inline fun <S, reified T : Action<S>> registerActionCallback(noinline callback: (T) -> Unit) =
        eventDispatcher.registerActionCallback(callback)
}

fun main() {
    if (!NetUtils.checkServerListening("localhost", 8454, 200)) {
//        startServer(GameService::class.java).ifPresent { server: Process ->
//            println("Starting Server")
//            Runtime.getRuntime().addShutdownHook(
//                    Thread {
//                        server.destroy()
//                        println("Stopped Server")
//                    }
//            )
//        }

        main(arrayOf("--port=8454", "--services=" + GameService::class.java.canonicalName))
    }

    PApplet.main(Minesweeper::class.java)
}
