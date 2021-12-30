package space.kiibou

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import processing.core.PApplet
import space.kiibou.data.Color
import space.kiibou.data.GREEN
import space.kiibou.data.RED
import space.kiibou.data.WHITE
import space.kiibou.event.MouseAction
import space.kiibou.event.MouseButton
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.FontRegistry
import space.kiibou.gui.text.TextElement
import space.kiibou.gui.text.TextFlow

class TestMain : GApplet() {
    override fun settings() {
        setScale(2)
        size(1200, 800, G2D)

        FontRegistry.register("fonts/NBP Informa FiveSix.ttf")
    }

    override fun setup() {
        frameRate(30f)

        val list = VerticalList(this, 3)
        list += TextElement(this, "Hello!", fontColor = RED)
        list += TextElement(this, "Hello, World!", fontColor = GREEN)
        list += TextElement(this, "Hello, Text Element!", fontColor = Color(35, 100, 194))
        list += TextElement(
            this,
            "list.addChild(TextElement(app, \"Hi\"));",
            fontSize = 15,
            fontName = "NBP Informa FiveSix",
            fontColor = WHITE
        )
        list += Button(this).also { button ->
            button += TextElement(this, "Test Button", fontName = "NBP Informa FiveSix", fontSize = 25).also { text ->
                val count = SimpleIntegerProperty(0)
                text.textProperty.bind(Bindings.concat("Click count: ", count))
                button.registerCallback(options(MouseButton.LEFT, MouseAction.RELEASE)) {
                    count.set(count.value + 1)
                }
            }
        }

        list += TextFlow(
            this,
            """
                Hello!
                Hello, World!
                Hello, Text Element!
                list.addChild(TextElement("Hi!"));
                This is a really really really loooooong text
                Click Count: 15
            """.trimIndent(), fontName = "NBP Informa FiveSix", fontSize = 25
        )

        list += TextElement(
            this,
            "Lorem ipsum dolor sit amet, consectetur adipisicing elit. 0123456789",
            fontSize = 25,
            fontName = "NBP Informa FiveSix"
        )

        registerGraphicsElement(list)
    }

    override fun draw() {
        background(0xCC)
    }
}

fun main() {
    PApplet.main(TestMain::class.java)
}
