package space.kiibou.gui

import processing.core.PGraphics

typealias TileRenderer = (PGraphics, Int, Int, Int, Int) -> Unit

enum class BorderStyle(location: String) {
    IN("border_box/border_in_tilemap.png"),
    OUT("border_box/border_out_tilemap.png");

    val corner1: TileRenderer
    val corner2: TileRenderer
    val corner3: TileRenderer
    val corner4: TileRenderer
    val border1: TileRenderer
    val border2: TileRenderer
    val border3: TileRenderer
    val border4: TileRenderer
    val center: TileRenderer
    val borderWidth: Int
    val borderHeight: Int

    init {
        val tilemap = loadImage(location)
        val factory = tilemapRenderFactory(tilemap)
        borderWidth = 2
        borderHeight = 2
        corner1 = factory(0, 0, borderWidth, borderHeight)
        border1 = factory(borderWidth, 0, 1, borderHeight)
        corner2 = factory(borderWidth + 1, 0, borderWidth, borderHeight)
        border2 = factory(borderWidth + 1, borderHeight, borderWidth, 1)
        corner3 = factory(borderWidth + 1, borderHeight + 1, borderWidth, borderHeight)
        border3 = factory(borderWidth, borderHeight + 1, 1, borderHeight)
        corner4 = factory(0, borderHeight + 1, borderWidth, borderHeight)
        border4 = factory(0, borderHeight, borderWidth, 1)
        center = factory(borderWidth, borderHeight, 1, 1)
    }
}