package space.kiibou.gui

import space.kiibou.GApplet

class VerticalList(app: GApplet, x: Int, y: Int, scale: Int) : GraphicsElement(app, x, y, 0, 0, scale) {
    override fun preInitImpl() {}
    public override fun initImpl() {
        var height = 0

        children.forEach {
            it.moveTo(x, y + height)
            height += it.height
        }

        var width = 0
        if (children.size > 0) {
            width = children.map(GraphicsElement::width).max() ?: 0
            for (child in children) child.width = width
        }

        resize(width, height)
    }

    override fun postInitImpl() {}
    override fun drawImpl() {}

    override fun addChild(element: GraphicsElement) {
        val borderBox = BorderBox(app, scale)
        borderBox += element
        super.addChild(borderBox)
    }

    override fun get(index: Int): BorderBox {
        return super.get(index) as BorderBox
    }
}