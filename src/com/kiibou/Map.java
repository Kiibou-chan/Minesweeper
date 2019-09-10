package com.kiibou;


import space.kiibou.GApplet;
import space.kiibou.gui.GraphicsElement;
import space.kiibou.gui.Grid;
import space.kiibou.gui.VerticalList;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Map extends GraphicsElement {
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
    /**
     * stores all tiles
     */
    private Grid<Tile> tiles;
    /**
     * reference to all bomb tiles
     */
    private Tile[] bombTiles;
    private VerticalList verticalList;
    private ControlBar controlBar;
    private boolean gameRunning;
    private int revealed;

    public Map(final GApplet app, final int x, final int y, final int tilesX, final int tilesY, final int scale, final int bombs) {
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
        if (revealed == tilesX * tilesY - bombs) {
            win();
        }
    }

    private Grid<Tile> createTiles(final int tilesX, final int tilesY) {
        final Grid<Tile> tiles = new Grid<>(getApp(), 0, 0, tilesX, tilesY, getScale());

        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                final Tile tile = new Tile(getApp(), this, getScale(), x, y);
                tiles.put(x, y, tile);
            }
        }

        return tiles;
    }

    /**
     * Places the specified amount of Bombs on the map.
     *
     * @param bombs Bombs to be placed
     * @return array of the Tiles on which bombs are placed
     * @throws IllegalArgumentException This exception is thrown when the amount
     *                                  of available tiles is lower than the specified amount of bombs
     */
    private Tile[] placeBombs(int bombs) {
        if (bombs > tilesX * tilesY)
            throw new IllegalArgumentException("Can not place more Bombs than Tiles are on the Map");

        final List<Tile> freeTiles = StreamSupport.stream(tiles.spliterator(), false)
                .collect(Collectors.toList());

        Collections.shuffle(freeTiles);

        return freeTiles.stream()
                .limit(bombs)
                .peek(bomb -> bomb.setType(TileType.BOMB)).toArray(Tile[]::new);
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
     * Reveal a tile at position x, y if it exists.
     * If it is empty, it reveals the surrounding tiles.
     *
     * @param x x-coordinate of the tile
     * @param y y-coordinate of the tile
     */
    public void reveal(int x, int y) {
        /* Go through each of the surrounding tiles and reveal them if tile is empty
         * otherwise just reveal the tile */
        if (isValidTile(x, y)) {
            if (tiles.get(x, y).getType() == TileType.EMPTY) {
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
        // TODO: 07/09/2019 refactor following if out of this method
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
