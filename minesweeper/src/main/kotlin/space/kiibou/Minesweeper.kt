@file:JvmName("MinesweeperMain")

package space.kiibou

import mu.KotlinLogging
import processing.core.PApplet
import processing.opengl.PGraphicsOpenGL
import processing.opengl.PJOGL
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.game.Map
import space.kiibou.net.NetUtils
import space.kiibou.net.client.Client
import space.kiibou.net.common.*
import space.kiibou.net.server.main
import space.kiibou.net.server.startServer
import space.kiibou.server.GameService

private val logger = KotlinLogging.logger { }

class Minesweeper : GApplet() {

    companion object {
        init {
            Serial.addModule(ClientMessageType.serializersModule)
            Serial.addModule(ServerMessageType.serializersModule)
            Serial.addModule(MinesweeperMessageType.serializersModule)
        }
    }

    private lateinit var map: Map
    lateinit var client: Client

    override fun settings() {
        size(800, 800, G2D)
        setScale(2)
        PJOGL.setIcon("pictures/icon_30.png")
    }

    override fun setup() {
        surface.setTitle("Minesweeper")
        surface.setResizable(true)
        (g as PGraphicsOpenGL).textureSampling(2)
        frameRate(60f)
        map = Map(this, 18, 18, 40)
        registerGraphicsElement(map)

        client = Client(
            ::onServerConnect,
            eventDispatcher::messageEvent,
            ::onServerDisconnect
        ).connect("localhost", 8454)
    }

    private fun onServerConnect() {
    }

    private fun onServerDisconnect() {
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

    fun <T : Any> onMessage(type: MessageType<T>, callback: (Message<T>) -> Unit) =
        eventDispatcher.onMessage(type, callback)

}

fun main() {
    if (!NetUtils.checkServerListening("localhost", 8454, 200)) {
        startServer().ifPresent { server: Process ->
            logger.info { "Starting Server" }

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    server.destroy()

                    logger.info { "Stopped Server" }
                }
            )
        }
    }

    PApplet.main(Minesweeper::class.java)
}
