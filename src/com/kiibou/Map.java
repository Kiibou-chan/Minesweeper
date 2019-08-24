package com.kiibou;


import space.kiibou.GApplet;
import space.kiibou.gui.GraphicsElement;
import space.kiibou.gui.Grid;
import space.kiibou.gui.VerticalList;

import java.util.function.Consumer;

public class Map extends GraphicsElement {
    /**
     * stores all tiles
     */
    private Grid<Tile> tiles;

    /**
     * reference to all bomb tiles
     */
    private Tile[] bombTiles;

    /**
     * amount of tiles in x direction
     */
    private final int tilesX;

    /**
     * amount of tiles in y direction
     */
    private final int tilesY;

    /**
     * amount of bombs on the field
     */
    private final int bombs;

    private VerticalList verticalList;
    private ControlBar controlBar;
    private boolean gameRunning;
    private int revealed;

    public Map(GApplet app, int x, int y, int tilesX, int tilesY, int scale, int bombs) {
        super(app, x, y, tilesX * Tile.tileHeight * scale, tilesY * Tile.tileHeight * scale, scale);
        this.tilesX = tilesX;
        this.tilesY = tilesY;
        this.bombs = bombs;
        gameRunning = false;
        revealed = 0;
    }

    @Override
    public void preInitImpl() {
        verticalList = new VerticalList(getApp(), getX(), getY(), getScale());
        addChild(verticalList);

        controlBar = new ControlBar(getApp(), getScale(), this);
        verticalList.addChild(controlBar);

        tiles = createTiles(tilesX, tilesY);
        verticalList.addChild(tiles);
    }

    @Override
    public void initImpl() {
        bombTiles = placeBombs(bombs);
        createNumberTiles();

        controlBar.setWidth(verticalList.getChild(0).getInnerWidth());
        resize(verticalList.getWidth(), verticalList.getHeight());
    }

    @Override
    public void postInitImpl() {

    }

    @Override
    public void drawImpl() {

    }

    private Grid<Tile> createTiles(int tilesX, int tilesY) {
        Grid<Tile> tiles = new Grid<>(getApp(), 0, 0, tilesX, tilesY, getScale());

        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                Tile tile = new Tile(getApp(), this, getScale(), x, y);
                tiles.put(x, y, tile);
            }
        }

        return tiles;
    }

    private Tile[] placeBombs(int bombs) {
        Tile[] bombTiles = new Tile[bombs];

        int placedBombs = 0;

        while (placedBombs < bombs) {
            int cx = (int) getApp().random(tilesX);
            int cy = (int) getApp().random(tilesY);
            Tile cur = tiles.get(cx, cy);

            while (cur.getType() == TileType.BOMB) {
                cx = (int) getApp().random(tilesX);
                cy = (int) getApp().random(tilesY);
                cur = tiles.get(cx, cy);
            }

            cur.setType(TileType.BOMB);
            bombTiles[placedBombs] = cur;
            placedBombs++;
        }

        return bombTiles;
    }

    private void createNumberTiles() {
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                if (isBomb(x, y)) continue;
                int count = 0;

                for (int px = -1; px <= 1; px++) {
                    for (int py = -1; py <= 1; py++) {
                        if (isBomb(x + px, y + py)) {
                            count++;
                        }
                    }
                }

                Tile cur = tiles.get(x, y);

                switch (count) {
                    case 0:
                        cur.setType(TileType.EMPTY);
                        break;
                    case 1:
                        cur.setType(TileType.ONE);
                        break;
                    case 2:
                        cur.setType(TileType.TWO);
                        break;
                    case 3:
                        cur.setType(TileType.THREE);
                        break;
                    case 4:
                        cur.setType(TileType.FOUR);
                        break;
                    case 5:
                        cur.setType(TileType.FIVE);
                        break;
                    case 6:
                        cur.setType(TileType.SIX);
                        break;
                    case 7:
                        cur.setType(TileType.SEVEN);
                        break;
                    case 8:
                        cur.setType(TileType.EIGHT);
                        break;
                }
            }
        }
    }

    private boolean isBomb(int x, int y) {
        if (isValidTile(x, y)) return (tiles.get(x, y).getType() == TileType.BOMB);
        else return false;
    }

    /**
     * Reveals a tile at position x, y if it exists.
     * Reveals surrounding empty or number tiles if it is empty.
     *
     * @param x x-coordinate of the tile
     * @param y y-coordinate of the tile
     */
    public void reveal(int x, int y) {
        if (isValidTile(x, y) && tiles.get(x, y).getType() == TileType.EMPTY) {
            for (int px = -1; px <= 1; px++) {
                for (int py = -1; py <= 1; py++) {
                    if (revealTile(x + px, y + py)) {
                        reveal(x + px, y + py);
                    }
                }
            }
        } else {
            revealTile(x, y);
        }

        if (revealed == tilesX * tilesY - bombs) {
            win();
        }
    }

    /**
     * Reveals the tile at position x, y if it exists.
     *
     * @param x x-coordinate of the tile
     * @param y y-coordinate of the tile
     * @return true if tile is empty, false otherwise
     */
    private boolean revealTile(int x, int y) {
        if (!gameRunning) {
            gameRunning = true;
            controlBar.startTimer();
        }

        boolean val = false;

        if (isValidTile(x, y)) {
            Tile tile = tiles.get(x, y);
            if (!tile.isRevealed()) {
                if (tile.getType() == TileType.EMPTY)
                    val = true;
                tile.setRevealed(true);
                revealed++;
            }
        }

        return val;
    }

    private void win() {
        controlBar.setSmiley(SmileyStatus.GLASSES);
        controlBar.stopTimer();

        forEachTile(Tile::deactivate);

        for (Tile b : bombTiles) {
            b.setFlagged(true);
        }

        gameRunning = false;
    }

    void loose() {
        for (Tile b : bombTiles) {
            b.setRevealed(true);
        }

        for (Tile t : tiles) {
            t.deactivate();
        }

        controlBar.setSmiley(SmileyStatus.DEAD);
        controlBar.stopTimer();
        gameRunning = false;
    }

    public void restart() {
        forEachTile(Tile::reset);
        bombTiles = placeBombs(bombs);
        createNumberTiles();
        controlBar.resetTimer();
        controlBar.resetBombsLeft();

        revealed = 0;
    }

    public void tileFlag(boolean flagged) {
        if (flagged) {
            controlBar.getBombsLeft().dec();
        } else {
            controlBar.getBombsLeft().inc();
        }
    }

    private boolean isValidTile(final int x, final int y) {
        return x >= 0 && x < tilesX && y >= 0 && y < tilesY;
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
