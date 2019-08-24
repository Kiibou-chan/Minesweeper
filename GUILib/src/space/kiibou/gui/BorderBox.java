package space.kiibou.gui;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;
import space.kiibou.GApplet;

public class BorderBox extends GraphicsElement {
    private final PGraphics g;
    private boolean redraw;
    private PGraphics buffer;
    private BorderStyle borderStyle = BorderStyle.IN;

    public BorderBox(GApplet app, int scale) {
        super(app, 0, 0, 0, 0, scale);
        this.g = app.getGraphics();
        redraw = true;
    }

    @Override
    protected void preInitImpl() {

    }

    @Override
    public void initImpl() {
        int x1 = getChildren().stream().mapToInt(GraphicsElement::getX).min().orElse(0);
        int y1 = getChildren().stream().mapToInt(GraphicsElement::getY).min().orElse(0);
        int x2 = getChildren().stream().mapToInt(child -> child.getX() + child.getWidth()).max().orElse(0);
        int y2 = getChildren().stream().mapToInt(child -> child.getY() + child.getHeight()).max().orElse(0);

        setX(x1 - borderStyle.borderWidth * getScale());
        setY(y1 - borderStyle.borderHeight * getScale());
        setWidth(x2 - x1 + 2 * borderStyle.borderWidth * getScale());
        setHeight(y2 - y1 + 2 * borderStyle.borderHeight * getScale());
    }

    @Override
    protected void postInitImpl() {

    }

    @Override
    public void drawImpl() {
        if (redraw) {
            createBuffer();
            buffer.beginDraw();
            int w = getWidth();
            int h = getHeight();
            int bw = borderStyle.borderWidth * getScale();
            int bh = borderStyle.borderHeight * getScale();

            drawTile(0, 0, bw, bh, borderStyle.corner1);
            drawTile(w - bw, 0, bw, bh, borderStyle.corner2);
            drawTile(w - bw, h - bh, bw, bh, borderStyle.corner3);
            drawTile(0, h - bh, bw, bh, borderStyle.corner4);

            drawTile(bw, 0, w - 2 * bw, bh, borderStyle.border1);
            drawTile(w - bw, bh, bw, h - 2 * bh, borderStyle.border2);
            drawTile(bw, h - bh, w - 2 * bw, bh, borderStyle.border3);
            drawTile(0, bh, bw, h - 2 * bh, borderStyle.border4);

            drawTile(bw, bh, w - 2 * bw, h - 2 * bh, borderStyle.center);
            redraw = false;
            buffer.endDraw();
        }
        g.image(buffer, getX(), getY());
    }

    private void createBuffer() {
        buffer = getApp().createGraphics(getWidth(), getHeight(), PConstants.P2D);
        ((PGraphicsOpenGL) buffer).textureSampling(3);
        buffer.beginDraw();
        buffer.endDraw();
    }

    private void drawTile(int x, int y, int width, int height, TileRenderer renderer) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        renderer.accept(buffer, rectangle);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        redraw = true;
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        redraw = true;
    }

    public int getInnerWidth() {
        return getWidth() - borderStyle.borderWidth * getScale() * 2;
    }

    public int getInnerHeight() {
        return getHeight() - borderStyle.borderHeight * getScale() * 2;
    }

    public int getBorderWidth() {
        return borderStyle.borderWidth * getScale();
    }

    public int getBorderHeight() {
        return borderStyle.borderHeight * getScale();
    }

    public void setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle;
        redraw = true;
    }

    public BorderStyle getBorderStyle() {
        return borderStyle;
    }
}
