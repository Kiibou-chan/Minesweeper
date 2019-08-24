package space.kiibou.gui;

import processing.core.PGraphics;

import java.util.function.BiConsumer;

public interface TileRenderer extends BiConsumer<PGraphics, Rectangle> {
}
