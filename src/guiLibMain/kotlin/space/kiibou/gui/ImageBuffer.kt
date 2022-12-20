package space.kiibou.gui

import mu.KotlinLogging
import processing.awt.PImageAWT
import processing.core.PImage
import javax.imageio.ImageIO

private var buffer: HashMap<String, PImage> = HashMap()

private var logger = KotlinLogging.logger {  }

fun loadImage(path: String): PImage {
    return buffer.computeIfAbsent(path) {
        try {
            val stream = GraphicsElement::class.java.classLoader.getResourceAsStream(path)!!
            PImageAWT(ImageIO.read(stream))
        } catch (e: Exception) {
            logger.warn { "Error loading image <$path>" }
            PImage(-1, -1)
        }
    }
}
