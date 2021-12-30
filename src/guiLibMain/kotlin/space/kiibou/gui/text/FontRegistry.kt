package space.kiibou.gui.text

import processing.core.PApplet
import processing.core.PFont
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.*

object FontRegistry {
    private val fontBuffer: MutableMap<Pair<String, Int>, PFont> = WeakHashMap()

    fun register(file: String) {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        ge.registerFont(
            Font.createFont(
                Font.TRUETYPE_FONT,
                FontRegistry::class.java.classLoader.getResourceAsStream(file)
            )
        )
    }

    fun get(app: PApplet, name: String, size: Int): PFont {
        return fontBuffer.computeIfAbsent(name to size) {
            app.createFont(name, size.toFloat())
        }
    }
}
