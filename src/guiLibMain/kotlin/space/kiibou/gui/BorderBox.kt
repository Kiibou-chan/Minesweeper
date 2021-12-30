package space.kiibou.gui

import javafx.beans.binding.IntegerBinding
import javafx.beans.property.SimpleIntegerProperty
import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PImage
import processing.opengl.PGraphicsOpenGL
import space.kiibou.GApplet
import java.util.*

data class Resolution(val width: Int, val height: Int)

private val buffers: MutableMap<Pair<Resolution, BorderStyle>, PImage> = WeakHashMap()

class BorderBox(app: GApplet) : GraphicsElement(app) {
    private val g: PGraphics = app.graphics
    private var redraw: Boolean = true
    private lateinit var buffer: PImage

    var style = BorderStyle.IN
        set(value) {
            field = value
            redraw = true
        }

    val borderWidthProp: IntegerBinding = scaleProperty.multiply(style.borderWidth)
    val borderWidth: Int
        get() = borderWidthProp.intValue()

    val borderHeightProp: IntegerBinding = scaleProperty.multiply(style.borderHeight)
    val borderHeight: Int
        get() = borderHeightProp.intValue()

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

    override fun drawImpl() {
        if (redraw) {
            buffer = drawBuffer()
        }

        g.image(buffer, x.toFloat(), y.toFloat())
    }

    private fun drawBuffer(): PImage {
        val key = Resolution(width, height) to style

        if (buffers.containsKey(key)) {
            return buffers[key]!!
        }

        val renderer = getRenderer()

        renderer.beginDraw()
        val w = width
        val h = height
        val bw = borderWidth
        val bh = borderHeight
        drawTile(0, 0, bw, bh, style.corner1, renderer)
        drawTile(w - bw, 0, bw, bh, style.corner2, renderer)
        drawTile(w - bw, h - bh, bw, bh, style.corner3, renderer)
        drawTile(0, h - bh, bw, bh, style.corner4, renderer)
        drawTile(bw, 0, w - 2 * bw, bh, style.border1, renderer)
        drawTile(w - bw, bh, bw, h - 2 * bh, style.border2, renderer)
        drawTile(bw, h - bh, w - 2 * bw, bh, style.border3, renderer)
        drawTile(0, bh, bw, h - 2 * bh, style.border4, renderer)
        drawTile(bw, bh, w - 2 * bw, h - 2 * bh, style.center, renderer)
        redraw = false
        renderer.endDraw()

        val result = renderer[0, 0, width, height]

        buffers[key] = result

        return result
    }

    private fun getRenderer(): PGraphics {
        return app.createGraphics(width, height, PConstants.P2D).apply {
            (this as PGraphicsOpenGL).textureSampling(3)
            beginDraw()
            endDraw()
        }
    }

    private inline fun drawTile(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        tileRenderer: TileRenderer,
        graphics: PGraphics
    ) {
        tileRenderer(graphics, x, y, width, height)
    }

    fun bindProps(other: GraphicsElement): BorderBox {
        other.xProp.bind(xProp.add(borderWidthProp))
        other.yProp.bind(yProp.add(borderHeightProp))
        other.scaleProperty.bind(scaleProperty)
        innerWidthProp.bind(other.widthProp)
        innerHeightProp.bind(other.heightProp)
        return this
    }
}
