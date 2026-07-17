package space.kiibou.gui

import processing.core.PGraphics
import processing.core.PImage
import space.kiibou.GApplet

class Picture(app: GApplet, private val image: PImage) : GraphicsElement(app) {
    private val g: PGraphics = app.graphics

    constructor(app: GApplet, path: String) : this(app, loadImage(path))

    init {
        widthProp.bind(scaleProperty.multiply(image.width))
        heightProp.bind(scaleProperty.multiply(image.height))

        if (width == 0 || height == 0) throw IllegalArgumentException("Picture must have non-zero width and height")
    }

    override fun drawImpl() {
        g.image(image, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
    }

    fun subPicture(x: Int, y: Int, width: Int, height: Int): Picture {
        return Picture(app, image[x, y, width, height])
    }
}
