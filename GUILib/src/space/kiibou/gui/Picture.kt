package space.kiibou.gui

import processing.core.PGraphics
import processing.core.PImage
import space.kiibou.GApplet

class Picture(app: GApplet, private val image: PImage, scale: Int) : GraphicsElement(app, 0, 0, 0, 0, scale) {
    private val g: PGraphics = app.graphics

    constructor(app: GApplet, path: String, scale: Int) : this(app, loadImage(path), scale)

    override fun preInitImpl() {}
    override fun initImpl() {
        if (width == 0 || height == 0) {
            resizeUnscaled(image.width, image.height)
        }
    }

    override fun postInitImpl() {}
    override fun drawImpl() {
        g.image(image, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
    }

    fun subPicture(x: Int, y: Int, width: Int, height: Int): Picture {
        return Picture(app, image[x, y, width, height], scale)
    }
}