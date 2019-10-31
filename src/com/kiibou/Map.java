package com.kiibou;


import space.kiibou.GApplet;
import space.kiibou.gui.GraphicsElement;
import space.kiibou.gui.Grid;
import space.kiibou.gui.VerticalList;

import java.util.function.Consumer;

public class Map extends GraphicsElement {

    private final int tilesX;
    private final int tilesY;
    private final int bombs;
    private Grid<Tile> tiles;
    private VerticalList verticalList;
    private ControlBar controlBar;

    public Map(final GApplet app, final int x, final int y, final int tilesX, final int tilesY, final int scale, final int bombs) {
        super(app, x, y, tilesX * Tile.tileHeight * scale, tilesY * Tile.tileHeight * scale, scale);
        this.tilesX = tilesX;
        this.tilesY = tilesY;
        this.bombs = bombs;
    }

    @Override
    public void preInitImpl() {
        verticalList = new VerticalList(getApp(), getX(), getY(), getScale());
        addChild(verticalList);

        controlBar = new ControlBar(getApp(), getScale(), this);
        verticalList.addChild(controlBar);

        tiles = createGrid(tilesX, tilesY);
        verticalList.addChild(tiles);
    }

    @Override
    public void initImpl() {
        controlBar.setWidth(verticalList.getChild(0).getInnerWidth());
        resize(verticalList.getWidth(), verticalList.getHeight());
    }

    @Override
    public void postInitImpl() {

    }

    @Override
    public void drawImpl() {

    }

    private Grid<Tile> createGrid(final int tilesX, final int tilesY) {
        final Grid<Tile> tiles = new Grid<>(getApp(), 0, 0, tilesX, tilesY, getScale());

        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                final Tile tile = new Tile(getApp(), this, getScale(), x, y);
                tiles.put(x, y, tile);
            }
        }

        return tiles;
    }

    public void revealTile(int x, int y, TileType type) {
        tiles.get(x, y).setType(type);
        tiles.get(x, y).setRevealed(true);
    }

    public void win() {
        controlBar.setSmiley(SmileyStatus.GLASSES);

        forEachTile(Tile::deactivate);
    }

    public void loose() {
        forEachTile(Tile::deactivate);

        controlBar.setSmiley(SmileyStatus.DEAD);
    }

    public void restart() {
        forEachTile(Tile::reset);
        controlBar.setSmiley(SmileyStatus.NORMAL);
        controlBar.getBombsLeft().setValue(bombs);
    }

    public void tileFlag(final int x, final int y, final boolean flagged) {
        if (flagged) {
            controlBar.getBombsLeft().dec();
        } else {
            controlBar.getBombsLeft().inc();
        }

        tiles.get(x, y).setFlagged(flagged);
    }

    private void forEachTile(Consumer<Tile> action) {
        tiles.forEach(action);
    }

    public int getBombs() {
        return bombs;
    }

    public ControlBar getControlBar() {
        return controlBar;
    }
}
