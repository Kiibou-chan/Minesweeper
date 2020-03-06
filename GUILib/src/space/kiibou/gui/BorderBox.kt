package space.kiibou.gui

import javafx.beans.property.SimpleIntegerProperty
import processing.core.PConstants
import processing.core.PGraphics
import processing.opengl.PGraphicsOpenGL
import space.kiibou.GApplet

class BorderBox(app: GApplet) : GraphicsElement(app) {
    private val g: PGraphics = app.graphics
    private var redraw: Boolean = true
    private lateinit var buffer: PGraphics

    var borderStyle = BorderStyle.IN
        set(value) {
            field = value
            redraw = true
        }

    val borderWidthProp = scaleProp.multiply(borderStyle.borderWidth)
    val borderWidth: Int
        get() = borderWidthProp.value as Int

    val borderHeightProp = scaleProp.multiply(borderStyle.borderHeight)
    val borderHeight: Int
        get() = borderHeightProp.value as Int

    val innerWidthProp = SimpleIntegerProperty(0)
    val innerWidth: Int
        get() = innerWidthProp.intValue()

    val innerHeightProp = SimpleIntegerProperty(0)
    val innerHeight: Int
        get() = innerHeightProp.intValue()

    init {
        widthProp.bind(innerWidthProp.add(borderHeightProp.multiply(2)))
        heightProp.bind(innerHeightProp.add(borderHeightProp.multiply(2)))
        widthProp.addListener { _, _, _ -> redraw = true }
        heightProp.addListener { _, _, _ -> redraw = true }
    }

    override fun preInitImpl() {}
    public override fun initImpl() {}
    override fun postInitImpl() {}

    public override fun drawImpl() {
        if (redraw) {
            createBuffer()
            buffer.beginDraw()
            val w = width
            val h = height
            val bw = borderWidth
            val bh = borderHeight
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

    fun bindProps(other: GraphicsElement): BorderBox {
        other.xProp.bind(xProp.add(borderWidthProp))
        other.yProp.bind(yProp.add(borderHeightProp))
        other.scaleProp.bind(scaleProp)
        innerWidthProp.bind(other.widthProp)
        innerHeightProp.bind(other.heightProp)
        return this
    }

}