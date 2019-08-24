package space.kiibou.gui;

import processing.core.PImage;

import java.util.function.Function;

public enum BorderStyle {

    IN("border_box/border_in_tilemap.png"),
    OUT("border_box/border_out_tilemap.png");

    public final TileRenderer corner1;
    public final TileRenderer corner2;
    public final TileRenderer corner3;
    public final TileRenderer corner4;

    public final TileRenderer border1;
    public final TileRenderer border2;
    public final TileRenderer border3;
    public final TileRenderer border4;

    public final TileRenderer center;

    public final int borderWidth;
    public final int borderHeight;

    BorderStyle(String location) {
        PImage tilemap = ImageBuffer.loadImage(location);
        Function<Rectangle, TileRenderer> factory = GraphicsElement.tilemapRenderFactory(tilemap);

        borderWidth = 2;
        borderHeight = 2;

        corner1 = factory.apply(new Rectangle(0, 0, borderWidth, borderHeight));
        border1 = factory.apply(new Rectangle(borderWidth, 0, 1, borderHeight));
        corner2 = factory.apply(new Rectangle(borderWidth + 1, 0, borderWidth, borderHeight));
        border2 = factory.apply(new Rectangle(borderWidth + 1, borderHeight, borderWidth, 1));
        corner3 = factory.apply(new Rectangle(borderWidth + 1, borderHeight + 1, borderWidth, borderHeight));
        border3 = factory.apply(new Rectangle(borderWidth, borderHeight + 1, 1, borderHeight));
        corner4 = factory.apply(new Rectangle(0, borderHeight + 1, borderWidth, borderHeight));
        border4 = factory.apply(new Rectangle(0, borderHeight, borderWidth, 1));
        center = factory.apply(new Rectangle(borderWidth, borderHeight, 1, 1));
    }
}
