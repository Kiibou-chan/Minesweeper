package space.kiibou.gui

import processing.core.PConstants
import processing.core.PGraphics
import processing.opengl.PGraphicsOpenGL
import space.kiibou.GApplet

class BorderBox(app: GApplet, scale: Int) : GraphicsElement(app, 0, 0, 0, 0, scale) {
    private val g: PGraphics = app.graphics
    private var redraw: Boolean = true
    private lateinit var buffer: PGraphics
    var borderStyle = BorderStyle.IN
        set(value) {
            field = value
            redraw = true
        }

    override var width: Int
        get() = super.width
        set(value) {
            super.width = value
            redraw = true
        }

    override var height: Int
        get() = super.height
        set(value) {
            super.height = value
            redraw = true
        }

    override fun preInitImpl() {}
    public override fun initImpl() {
        val x1 = children.stream().mapToInt { it.x }.min().orElse(0)
        val y1 = children.stream().mapToInt { it.y }.min().orElse(0)
        val x2 = children.stream().mapToInt { it.x + it.width }.max().orElse(0)
        val y2 = children.stream().mapToInt { it.y + it.height }.max().orElse(0)
        x = x1 - borderStyle.borderWidth * scale
        y = y1 - borderStyle.borderHeight * scale
        width = x2 - x1 + 2 * borderStyle.borderWidth * scale
        height = y2 - y1 + 2 * borderStyle.borderHeight * scale
    }

    override fun postInitImpl() {}

    public override fun drawImpl() {
        if (redraw) {
            createBuffer()
            buffer.beginDraw()
            val w = width
            val h = height
            val bw = borderStyle.borderWidth * scale
            val bh = borderStyle.borderHeight * scale
            drawTile(0, 0, bw, bh, borderStyle.corner1)
            drawTile(w - bw, 0, bw, bh, borderStyle.corner2)
            drawTile(w - bw, h - bh, bw, bh, borderStyle.corner3)
            drawTile(0, h - bh, bw, bh, borderStyle.corner4)
            drawTile(bw, 0, w - 2 * bw, bh, borderStyle.border1)
            drawTile(w - bw, bh, bw, h - 2 * bh, borderStyle.border2)
            drawTile(bw, h - bh, w - 2 * bw, bh, borderStyle.border3)
            drawTile(0, bh, bw, h - 2 * bh, borderStyle.border4)
            drawTile(bw, bh, w - 2 * bw, h - 2 * bh, borderStyle.center)
            redraw = false
            buffer.endDraw()
        }
        g.image(buffer, x.toFloat(), y.toFloat())
    }

    private fun createBuffer() {
        buffer = app.createGraphics(width, height, PConstants.P2D)
        (buffer as PGraphicsOpenGL).textureSampling(3)
        buffer.beginDraw()
        buffer.endDraw()
    }

    private inline fun drawTile(x: Int, y: Int, width: Int, height: Int, renderer: TileRenderer) {
        renderer(buffer, x, y, width, height)
    }

    val innerWidth: Int
        get() = width - borderStyle.borderWidth * scale * 2

    val innerHeight: Int
        get() = height - borderStyle.borderHeight * scale * 2

    val borderWidth: Int
        get() = borderStyle.borderWidth * scale

    val borderHeight: Int
        get() = borderStyle.borderHeight * scale

}