import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import processing.core.PApplet
import space.kiibou.GApplet
import space.kiibou.data.Color
import space.kiibou.data.GREEN
import space.kiibou.data.RED
import space.kiibou.data.WHITE
import space.kiibou.event.MouseAction
import space.kiibou.event.MouseButton
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.GGraphics
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.TextElement
import space.kiibou.gui.text.TextFlow

class TestMain : GApplet() {
    override fun settings() {
        setScale(2)
        size(1200, 800, GGraphics::class.java.canonicalName)
    }

    override fun setup() {
        frameRate(30f)

        val list = VerticalList(this, 3)
        list.addChild(TextElement(this, "Hello!", fontColor = RED))
        list.addChild(TextElement(this, "Hello, World!", fontColor = GREEN))
        list.addChild(TextElement(this, "Hello, Text Element!", fontColor = Color(35, 100, 194)))
        list.addChild(TextElement(this, "list.addChild(TextElement(", fontName = "Consolas", fontColor = WHITE, fontSize = 15))
        list.addChild(Button(this).also { button ->
            button.addChild(TextElement(this, "Test Button", fontName = "Segoe UI", fontSize = 12).also { text ->
                val count = SimpleIntegerProperty(0)
                text.textProperty.bind(Bindings.concat("Test Button ", count))
                button.registerCallback(options(MouseButton.LEFT, MouseAction.RELEASE)) {
                    count.set(count.value + 1)
                }
            })
        })
        registerGraphicsElement(list)

        val text = TextFlow(this, """Hello!
Hello, World!
Hello, Text Element!
list.addChild(TextElement(""", fontColor = WHITE, fontName = "Consolas")
        registerGraphicsElement(text)
    }

    override fun draw() {
        background(0x27)
    }
}

fun main() {
    PApplet.main(TestMain::class.java)
}