package space.kiibou.gui.text

import javafx.beans.binding.IntegerBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import processing.core.PConstants.BOTTOM
import processing.core.PConstants.LEFT
import processing.core.PFont
import space.kiibou.GApplet
import space.kiibou.data.BLACK
import space.kiibou.data.Color
import space.kiibou.gui.GraphicsElement

private val fontBuffer: MutableMap<Pair<String, Int>, PFont> = HashMap()

private fun createFont(app: GApplet, name: String, size: Int): PFont {
    return fontBuffer.computeIfAbsent(name to size) {
        app.createFont(name, size.toFloat())
    }
}

class TextElement(app: GApplet, text: String, fontSize: Int = 15, fontName: String = "Times New Roman", fontColor: Color = BLACK) : GraphicsElement(app) {
    val fontSizeProperty = SimpleIntegerProperty(0).also {
        it.bind(scaleProp.multiply(fontSize))
    }
    val fontNameProperty = SimpleStringProperty(fontName)
    val fontProperty = object : ObjectBinding<PFont>() {
        init {
            bind(fontNameProperty, fontSizeProperty)
        }

        override fun computeValue() = createFont(app, fontName, fontSizeProperty.value.toInt())
    }

    val fontColorProperty = SimpleObjectProperty(fontColor)
    val textProperty = SimpleStringProperty(text.replace("\n", ""))

    init {
        widthProp.bind(object : IntegerBinding() {
            init {
                bind(textProperty, scaleProp, fontProperty)
            }

            override fun computeValue(): Int {
                app.pushStyle()
                app.textFont(fontProperty.value)
                val w = app.textWidth(textProperty.valueSafe).toInt()
                app.popStyle()
                return w
            }
        })

        heightProp.bind(fontSizeProperty)
    }

    override fun drawImpl() {
        with(app.gg) {
            textFont(fontProperty.value)
            textAlign(LEFT, BOTTOM)
            fill(fontColorProperty.value)
            text(textProperty.valueSafe, x.toFloat(), y.toFloat() + this@TextElement.height)
        }
    }

}