package space.kiibou.gui

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableNumberValue
import space.kiibou.GApplet

class VerticalList(app: GApplet, x: Int, y: Int, scale: Int) : GraphicsElement(app, x, y, 0, 0, scale) {

    init {
        childrenProperty.addListener { _, _, list ->
            var h: ObservableNumberValue = SimpleIntegerProperty(0)
            list.forEach {
                it.xProp.bind(xProp)
                it.yProp.bind(h)
                h = Bindings.add(it.heightProp, h)
            }
            heightProp.bind(h)

            var w: ObservableNumberValue = SimpleIntegerProperty(0)
            list.forEach {
                w = Bindings.max(it.widthProp, w)
            }
            widthProp.bind(w)
        }
    }

    override fun preInitImpl() {}
    public override fun initImpl() {}
    override fun postInitImpl() {}
    override fun drawImpl() {}

}