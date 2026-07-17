package space.kiibou

import processing.core.PApplet
import space.kiibou.data.*
import space.kiibou.event.MouseAction
import space.kiibou.event.MouseButton
import space.kiibou.event.options
import space.kiibou.gui.Button
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.FontRegistry
import space.kiibou.gui.text.TextElement
import space.kiibou.gui.text.TextFlow
import space.kiibou.reactive.count
import space.kiibou.reactive.map
import space.kiibou.reactive.reactives.Evt
import space.kiibou.reactive.reactives.fire

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
            fontName = "Times New Roman",
            fontColor = WHITE
        )

        list += Button(this).also { button ->
            button += TextElement(this, "Test Button", fontName = "NBP Informa FiveSix", fontSize = 25).also { text ->
                val clickedEvt = Evt<Unit>()

                text.textProperty.bind(clickedEvt.count.map { "Click count: $it" }.toFX)

                button.registerCallback(options(MouseButton.LEFT, MouseAction.RELEASE)) {
                    clickedEvt.fire()
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
                Click Count: 15
            """.trimIndent(),
            fontName = "NBP Informa FiveSix",
            fontSize = 25
        )

        list += TextElement(
            this,
            "Lorem ipsum dolor sit amet, consectetur adipisicing elit. 0123456789",
            fontSize = 25,
            fontName = "NBP Informa FiveSix"
        )

        list.x = 10
        list.y = 10

        registerGraphicsElement(list)
    }

    override fun draw() {
        background(0xCC)
    }
}

fun main() {
    PApplet.main(TestMain::class.java)
}
