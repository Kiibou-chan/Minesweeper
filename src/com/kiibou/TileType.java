package com.kiibou;

public enum TileType {

    RED_BOMB("tiles/red_bomb_tile.png"),
    BOMB("tiles/bomb_tile.png"),
    EMPTY("tiles/empty_tile.png"),
    ONE("tiles/one_tile.png"),
    TWO("tiles/two_tile.png"),
    THREE("tiles/three_tile.png"),
    FOUR("tiles/four_tile.png"),
    FIVE("tiles/five_tile.png"),
    SIX("tiles/six_tile.png"),
    SEVEN("tiles/seven_tile.png"),
    EIGHT("tiles/eight_tile.png");

    private final String path;

    TileType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
