@file:JvmName("MinesweeperMain")

package space.kiibou

import mu.KotlinLogging
import processing.core.PApplet
import processing.opengl.PGraphicsOpenGL
import processing.opengl.PJOGL
import space.kiibou.common.MinesweeperMessageType
import space.kiibou.common.NameInfo
import space.kiibou.common.ReadyInfo
import space.kiibou.common.RoomPhase
import space.kiibou.common.RoomStateInfo
import space.kiibou.game.Map
import space.kiibou.gui.ScreenManager
import space.kiibou.lobby.MainMenuScreen
import space.kiibou.lobby.RoomListScreen
import space.kiibou.lobby.RoomLobbyScreen
import space.kiibou.net.NetUtils
import space.kiibou.net.client.Client
import space.kiibou.net.common.*
import space.kiibou.net.server.startServer

private val logger = KotlinLogging.logger { }

class Minesweeper : GApplet() {

    companion object {
        init {
            Serial.addModule(ClientMessageType.serializersModule)
            Serial.addModule(ServerMessageType.serializersModule)
            Serial.addModule(MinesweeperMessageType.serializersModule)
        }
    }

    lateinit var client: Client

    private lateinit var screens: ScreenManager
    private lateinit var mainMenuScreen: MainMenuScreen
    private lateinit var roomListScreen: RoomListScreen
    private lateinit var lobbyScreen: RoomLobbyScreen

    private var map: Map? = null
    private var myId: Long? = null
    private var lastRoomState: RoomStateInfo? = null

    override fun settings() {
        size(800, 800, G2D)
        setScale(2)
        pixelDensity(1)
        PJOGL.setIcon("pictures/icon_30.png")
    }

    override fun setup() {
        surface.setTitle("Minesweeper")
        surface.setResizable(true)
        (g as PGraphicsOpenGL).textureSampling(2)
        frameRate(60f)

        screens = ScreenManager(this)
        mainMenuScreen = MainMenuScreen(this).also(screens::add)
        roomListScreen = RoomListScreen(this).also(screens::add)
        lobbyScreen = RoomLobbyScreen(this).also(screens::add)
        screens.show(mainMenuScreen)

        registerMessageHandlers()

        // Assign before connecting: onServerConnect uses the client field and connect()
        // invokes it synchronously.
        client = Client(
            ::onServerConnect,
            eventDispatcher::messageEvent,
            ::onServerDisconnect
        )
        client.connect("localhost", 8454)
    }

    private fun registerMessageHandlers() {
        onMessage(MinesweeperMessageType.RoomList) { roomListScreen.update(it.payload.rooms) }

        onMessage(MinesweeperMessageType.YourId) {
            myId = it.payload.id
            lastRoomState?.let { state -> lobbyScreen.update(state, myId) }
        }

        onMessage(MinesweeperMessageType.RoomState) {
            lastRoomState = it.payload
            lobbyScreen.update(it.payload, myId)
            if (screens.current == roomListScreen || screens.current == mainMenuScreen) {
                screens.show(lobbyScreen)
            }
        }

        onMessage(MinesweeperMessageType.JoinRefused) { client.send(MinesweeperMessageType.ListRooms) }

        onMessage(MinesweeperMessageType.GameStarted) { showGameScreen() }

        onMessage(MinesweeperMessageType.SetTime) { map?.controlBar?.timerDisplay?.value = it.payload.time }
        onMessage(MinesweeperMessageType.RevealTiles) { map?.revealTiles(it.payload.tiles) }
        onMessage(MinesweeperMessageType.SetFlag) { map?.setFlag(it.payload.x, it.payload.y, it.payload.status) }
        onMessage(MinesweeperMessageType.SetBombsLeft) { map?.controlBar?.bombsLeft?.value = it.payload.bombs }
        onMessage(MinesweeperMessageType.Win) { map?.onWin() }
        onMessage(MinesweeperMessageType.Loose) { map?.onLose() }
        onMessage(MinesweeperMessageType.Restart) { map?.onRestart() }
    }

    private fun showGameScreen() {
        val settings = lastRoomState?.settings ?: return

        map = Map(this, settings.width, settings.height, settings.bombs).also {
            screens.add(it)
            screens.show(it)
        }
    }

    /** The room is back in its lobby after a game; the smiley is the way back to it. */
    fun onSmileyClicked() {
        if (lastRoomState?.phase == RoomPhase.LOBBY && screens.current == map) {
            screens.show(lobbyScreen)
        }
    }

    fun setPlayerName(name: String) {
        if (name.isNotBlank()) client.send(MinesweeperMessageType.SetName, NameInfo(name.trim()))
    }

    fun startSingleplayer() {
        client.send(MinesweeperMessageType.CreateRoom)
        client.send(MinesweeperMessageType.SetReady, ReadyInfo(true))
    }

    fun showMainMenu() {
        screens.show(mainMenuScreen)
    }

    fun showRoomList() {
        screens.show(roomListScreen)
        client.send(MinesweeperMessageType.ListRooms)
    }

    private fun onServerConnect() {
    }

    private fun onServerDisconnect() {
        exit()
    }

    override fun draw() {
        screens.current?.let { screen ->
            if (screen.width in 1 until width || screen.height in 1 until height) {
                val cX = width / 2 - screen.width / 2
                val cY = height / 2 - screen.height / 2
                if (screen.x != cX || screen.y != cY) screen.moveTo(cX, cY)
            }

            if (width < screen.width) surface.setSize(screen.width, height)
            if (height < screen.height) surface.setSize(width, screen.height)
        }

        background(0xCC)
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
