package space.kiibou.gui;

import processing.core.PGraphics;
import processing.core.PImage;
import space.kiibou.GApplet;

public class Picture extends GraphicsElement {
    private final PImage image;
    private final PGraphics g;

    public Picture(GApplet app, String path, int scale) {
        this(app, ImageBuffer.loadImage(path), scale);
    }

    public Picture(GApplet app, PImage image, int scale) {
        super(app, 0, 0, 0, 0, scale);
        this.g = app.getGraphics();
        this.image = image;
    }

    @Override
    protected void preInitImpl() {

    }

    @Override
    protected void initImpl() {
        if (getWidth() == 0 || getHeight() == 0) {
            resizeUnscaled(image.width, image.height);
        }
    }

    @Override
    protected void postInitImpl() {
    }

    @Override
    protected void drawImpl() {
        g.image(image, getX(), getY(), getWidth(), getHeight());
    }

    public Picture subPicture(int x, int y, int width, int height) {
        return new Picture(getApp(), image.get(x, y, width, height), getScale());
    }

}
