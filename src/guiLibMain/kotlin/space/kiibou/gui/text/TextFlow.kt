package space.kiibou.gui.text

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableNumberValue
import space.kiibou.GApplet
import space.kiibou.data.BLACK
import space.kiibou.data.Color
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.VerticalList

class TextFlow(app: GApplet, text: String, fontColor: Color = BLACK, fontName: String = "Times New Roman", fontSize: Int = 15) : GraphicsElement(app) {
    val fontColorProperty = SimpleObjectProperty(fontColor)
    val fontSizeProperty: ObservableNumberValue = scaleProp.multiply(fontSize)
    val fontNameProperty = SimpleStringProperty(fontName)

    private val list = VerticalList(app).also {
        widthProp.bind(it.widthProp)
        heightProp.bind(it.heightProp)
        it.xProp.bind(xProp)
        it.yProp.bind(yProp)
        addChild(it)
    }

    val textProperty = SimpleStringProperty("").also {
        it.addListener { _, _, new ->
            deferAfterDraw {
                list.removeAllChildren()
                val lines = new.lines()
                for (line in lines) {
                    list.addChild(TextElement(app, line).also { text ->
                        text.fontSizeProperty.bind(fontSizeProperty)
                        text.fontNameProperty.bind(fontNameProperty)
                        text.fontColorProperty.bind(fontColorProperty)
                    })
                }
            }
        }
    }

    init {
        textProperty.value = text
    }

}