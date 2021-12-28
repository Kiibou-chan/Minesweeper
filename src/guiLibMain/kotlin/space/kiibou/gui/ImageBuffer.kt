package space.kiibou.gui

import processing.awt.PImageAWT
import processing.core.PImage
import javax.imageio.ImageIO

private var buffer: HashMap<String, PImage> = HashMap()

fun loadImage(path: String): PImage {
    return buffer.computeIfAbsent(path) {
        try {
            val stream = GraphicsElement::class.java.classLoader.getResourceAsStream(path)!!
            PImageAWT(ImageIO.read(stream))
        } catch (e: Exception) {
            println("Error loading image <$path>")
            PImage(-1, -1)
        }
    }
}
