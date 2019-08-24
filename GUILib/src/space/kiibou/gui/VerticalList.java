package space.kiibou.gui;

import space.kiibou.GApplet;

public class VerticalList extends GraphicsElement {
    public VerticalList(GApplet app, int x, int y, int scale) {
        super(app, x, y, 0, 0, scale);
    }

    @Override
    protected void preInitImpl() {

    }

    @Override
    public void initImpl() {
        final int[] height = {0};
        getChildren().forEach(child -> {
            child.moveTo(getX(), getY() + height[0]);
            height[0] += child.getHeight();
        });

        int width = 0;
        if (getChildren().size() > 0) {
            width = getChildren().stream().mapToInt(GraphicsElement::getWidth).max().orElse(0);
            for (GraphicsElement child : getChildren()) child.setWidth(width);
        }
        resize(width, height[0]);
    }

    @Override
    protected void postInitImpl() {

    }

    @Override
    protected void drawImpl() {

    }

    @Override
    public void addChild(GraphicsElement element) {
        BorderBox borderBox = new BorderBox(getApp(), getScale());
        borderBox.addChild(element);
        super.addChild(borderBox);
    }

    @Override
    public BorderBox getChild(int index) {
        return (BorderBox) super.getChild(index);
    }

}
