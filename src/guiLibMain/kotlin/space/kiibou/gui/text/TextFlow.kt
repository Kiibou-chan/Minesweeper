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
    val fontSizeProperty: ObservableNumberValue = scaleProperty.multiply(fontSize)

    // TODO (Svenja, 28/12/2021): Currently the children do not use the correct font name. This is because the name is bound
    //  but the font in the text element is not reloaded. Temporary fix is employed by manually passing the font name to
    //  the constructor of the TextElement (see Line 35) but this will not account for the case where the font name of this
    //  element is changed later on.
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
                    list.addChild(TextElement(app, line, fontName = fontName).also { text ->
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
