package space.kiibou.gui.text

import javafx.beans.Observable
import javafx.beans.binding.IntegerBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import processing.core.PConstants.BOTTOM
import processing.core.PConstants.LEFT
import space.kiibou.GApplet
import space.kiibou.data.BLACK
import space.kiibou.data.Color
import space.kiibou.gui.GraphicsElement

class TextElement(
    app: GApplet,
    text: String,
    fontSize: Int = 15,
    fontName: String = "Times New Roman",
    fontColor: Color = BLACK
) : GraphicsElement(app) {
    val fontSizeProperty = SimpleIntegerProperty(0).also {
        it.bind(scaleProperty.multiply(fontSize))
    }

    val fontNameProperty = SimpleStringProperty(fontName)
    private val fontProperty = ObjectBinding(fontNameProperty, fontSizeProperty) {
        FontRegistry.get(app, fontName, fontSizeProperty.value)
    }

    val fontColorProperty = SimpleObjectProperty(fontColor)
    val textProperty = SimpleStringProperty(text)

    init {
        widthProp.bind(IntegerBinding(textProperty, fontSizeProperty, fontProperty) {
            app.gg.textWidth(fontProperty.value, fontSizeProperty.value, textProperty.valueSafe)
        })

        heightProp.bind(IntegerBinding(fontSizeProperty, fontProperty, textProperty) {
            app.gg.textHeight(fontProperty.value, fontSizeProperty.value, textProperty.valueSafe)
        })
    }

    override fun drawImpl() {
        with(app.gg) {
            textFont(fontProperty.value)
            textAlign(LEFT, BOTTOM)
            textLeading(textAscent() + textDescent())
            fill(fontColorProperty.value)
            text(textProperty.valueSafe, x.toFloat(), y.toFloat() + this@TextElement.height)
        }
    }
}

private fun <T> ObjectBinding(vararg observables: Observable, producer: () -> T): ObjectBinding<T> =
    object : ObjectBinding<T>() {
        init {
            bind(*observables)
        }

        override fun computeValue() = producer()
    }

private fun IntegerBinding(vararg observables: Observable, producer: () -> Int): IntegerBinding =
    object : IntegerBinding() {
        init {
            bind(*observables)
        }

        override fun computeValue() = producer()
    }
