package space.kiibou.gui

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableNumberValue
import space.kiibou.GApplet

class VerticalList(app: GApplet, x: Int, y: Int, margin: Int = 0, scale: Int) : GraphicsElement(app, x, y, 0, 0, scale) {

    private val marginProp = scaleProp.multiply(margin)

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

    override fun preInitImpl() {}
    public override fun initImpl() {}
    override fun postInitImpl() {}
    override fun drawImpl() {}

}