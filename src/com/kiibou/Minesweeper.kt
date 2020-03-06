package com.kiibou

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kiibou.server.GameService
import processing.core.PApplet
import processing.opengl.PGraphicsOpenGL
import processing.opengl.PJOGL
import space.kiibou.GApplet
import space.kiibou.gui.GGraphics
import space.kiibou.net.NetUtils
import space.kiibou.net.client.Client
import space.kiibou.net.server.main

class Minesweeper : GApplet() {
    private lateinit var map: Map
    lateinit var client: Client

    override fun settings() {
        size(800, 800, GGraphics::class.java.canonicalName)
        setScale(4)
        //        fullScreen(P2D)
        PJOGL.setIcon("pictures/icon.png")
    }

    override fun setup() {
        surface.setTitle("Minesweeper")
        (g as PGraphicsOpenGL).textureSampling(2)
        frameRate(60f)
        map = Map(this, 18, 18, 50)
        registerGraphicsElement(map)

        registerJsonCallback("set-time", ::setTime)
        registerJsonCallback("reveal-tiles", ::revealTiles)
        registerJsonCallback("win") { map.win() }
        registerJsonCallback("loose") { map.loose() }
        registerJsonCallback("restart") { map.restart() }
        registerJsonCallback("toggle-flag", ::toggleFlag)

        client = Client(
                ::onServerConnect,
                eventDispatcher::jsonEvent,
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

    private fun revealTiles(o: JsonNode) = map.revealTiles(mapper.treeToValue(o, RevealTiles::class.java))

    private fun setTime(o: JsonNode) {
        map.controlBar.timerDisplay.value = o.at("/time").intValue()
    }

    private fun toggleFlag(o: JsonNode) = map.tileFlag(mapper.treeToValue(o, FlagInfo::class.java))

    private fun registerJsonCallback(action: String, callback: (JsonNode) -> Unit) =
            eventDispatcher.registerJsonCallback(action, callback)
}

val mapper: ObjectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

data class TileInfo @JsonCreator constructor(
        @param:JsonProperty("x") val x: Int,
        @param:JsonProperty("y") val y: Int,
        @param:JsonProperty("type") val type: Int)

data class FlagInfo @JsonCreator constructor(
        @param:JsonProperty("x") val x: Int,
        @param:JsonProperty("y") val y: Int,
        @param:JsonProperty("toggle") val toggle: Boolean)

data class RevealTiles @JsonCreator constructor(
        @param:JsonProperty("tiles") val tiles: List<TileInfo>)

data class TimeInfo @JsonCreator constructor(
        @param:JsonProperty("time") val time: Int)

data class MapInfo @JsonCreator constructor(
        @param:JsonProperty("width") val width: Int,
        @param:JsonProperty("height") val height: Int,
        @param:JsonProperty("bombs") val bombs: Int,
        @param:JsonProperty("action") val action: String = "init-map")

fun main() {
    if (!NetUtils.checkServerListening("localhost", 8454, 200)) {
        /*
        startServer(GameService::class.java).ifPresent { server: Process ->
            println("Starting Server")
            Runtime.getRuntime().addShutdownHook(
                    Thread {
                        server.destroy()
                        println("Stopped Server")
                    }
            )
        }
         */

        main(arrayOf("--port=8454", "--services=" + GameService::class.java.canonicalName))
    }

    PApplet.main(Minesweeper::class.java)
}
