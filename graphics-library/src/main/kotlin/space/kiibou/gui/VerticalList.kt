package space.kiibou.gui

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableNumberValue
import space.kiibou.GApplet

class VerticalList(app: GApplet, margin: Int = 0) : GraphicsElement(app) {
    private val marginProp = scaleProperty.multiply(margin)

    init {
        childrenProperty.addListener { _, _, list ->
            var h: ObservableNumberValue = SimpleIntegerProperty(0)
            list.forEach {
                it.xProp.bind(xProp)
                it.yProp.bind(yProp.add(h))
                h = it.heightProp.add(h).add(marginProp)
            }
            heightProp.bind(Bindings.subtract(h, marginProp))

            var w: ObservableNumberValue = SimpleIntegerProperty(0)
            list.forEach { w = Bindings.max(it.widthProp, w) }
            widthProp.bind(w)
        }
    }
}
